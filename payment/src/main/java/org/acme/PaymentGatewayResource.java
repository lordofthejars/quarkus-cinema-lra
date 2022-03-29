package org.acme;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_CONTEXT_HEADER;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.lra.annotation.ParticipantStatus;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.jboss.logging.Logger;

@Path("/pay")
public class PaymentGatewayResource {
    
    @Inject
    Logger log;

    boolean misbehave;

    @ConfigProperty(name = "transaction.time")
    long time;

    @POST
    @LRA(value = LRA.Type.MANDATORY, end = true)
    public Response pay(Payment payment, @HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) throws InterruptedException {

        log.info("Paying " + payment.cardNumber);
        log.info(lraId);

        TimeUnit.SECONDS.sleep(time);

        return misbehave ? Response.serverError().build() : Response.ok().build();

    }

    @org.eclipse.microprofile.lra.annotation.Compensate
    @Path("/compensate")
    @PUT
    public Response compensateWork(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {

        log.info("Compensate Payment" + lraId.toASCIIString());
        return Response.ok(ParticipantStatus.Compensated.name()).build();
    }

    @GET
    @Path("/misbehave")
    public String misbehave() {
        misbehave = true;

        return "Misbehaving";
    }

    @GET
    @Path("/behave")
    public String behave() {
        misbehave = false;

        return "Behave";
    }
}