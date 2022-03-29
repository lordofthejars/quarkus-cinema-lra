package org.acme;

import java.net.URI;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.eclipse.microprofile.lra.annotation.ParticipantStatus;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.providers.sse.OutboundSseEventImpl;

import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_CONTEXT_HEADER;

@Path("/booking")
public class BookingResource {

    @Inject
    Logger log;

    @Inject
    ObjectMapper json;

    private volatile SseBroadcaster sseBroadcaster;

    @PUT
    @Path("/create/{id}")
    @LRA(value = LRA.Type.REQUIRES_NEW, end = false)
    public Response createBooking(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId,
                                  @PathParam("id") long seatId,
                                  Booking booking) {
        
        try  {
            booking.id = lraId.toASCIIString();

            log.info("Starting transaction " + booking.id);

            if (booking.persist(seatId)) {
                log.info("Creating booking for " + seatId);
                return Response.ok().build();
            } else {
                log.info("Seat " + seatId + " already booked!");
                return Response
                        .status(Response.Status.CONFLICT)
                        .entity(json.createObjectNode()
                                .put("error", "Seat " + seatId + " is already reserved!")
                                .put("seat", seatId)
                                .objectNode())
                        .build();
            }
        } catch (Exception e) {
            e.printStackTrace();

            return Response
                        .status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(json.createObjectNode()
                                .put("error", e.getMessage())
                                .put("seat", seatId)
                                .objectNode())
                        .build();
        }
    }

    @GET
    @Path("/seat")
    public List<Seat> getAllBookedSeats() {
        return Booking.findAllSeats();
    }

    @org.eclipse.microprofile.lra.annotation.Compensate
    @Path("/compensate")
    @PUT
    public Response paymentFailed(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {

        log.info("Compensating " + lraId.toASCIIString());

        Booking.findByIdOptional(lraId.toASCIIString())
        .ifPresent(b -> {
            b.delete();
            Long seatId = ((Booking)b).seat;
            log.info("Booking for seat " + seatId + " cleared!");
            if (sseBroadcaster != null) {
                sseBroadcaster.broadcast(
                        newEventBuilder()
                            .data(new Seat(seatId))
                            .mediaType(MediaType.APPLICATION_JSON_TYPE)
                            .build());
            }
        });

        return Response.ok(ParticipantStatus.Compensated.name()).build();
    }

    private OutboundSseEvent.Builder newEventBuilder() {
        return new OutboundSseEventImpl.BuilderImpl();
    }

    @org.eclipse.microprofile.lra.annotation.Complete
    @Path("/complete")
    @PUT
    public Response paymentSuccessful(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {

        System.out.println("TX complete book" + lraId.toASCIIString());

        log.info("Payment success! " + lraId);
        return Response.ok(ParticipantStatus.Completed.name()).build();
    }

    @GET
    @Path("sse-notifications")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void listenToEvents(@Context SseEventSink eventSink, @Context Sse sse) {
        if (this.sseBroadcaster == null) {
            this.sseBroadcaster = sse.newBroadcaster();
        }
        sseBroadcaster.register(eventSink);
    }
}