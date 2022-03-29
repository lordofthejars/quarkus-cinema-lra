package org.acme;

import java.net.URI;

import javax.inject.Inject;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.eclipse.microprofile.lra.annotation.ParticipantStatus;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@Path("/payment")
public class PaymentResource {
 
    @Inject
    Logger log;

    @RestClient
    PaymentGateway paymentGateway;

    @PUT
    @Path("/pay")
    @LRA(value = LRA.Type.MANDATORY, end = false)
    public Response makePayment(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId, ObjectNode paymentInfo) {
        
        try {
            final Response r = paymentGateway.pay(paymentInfo);
            log.info("Payment service repsonse: " + r.getStatus() + " LRA " + lraId);
        } catch (WebApplicationException e) {
            return Response.serverError().build();
        }
        return Response.accepted().build();

    }

    @org.eclipse.microprofile.lra.annotation.Compensate
    @Path("/compensate")
    @PUT
    public Response compensate(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        System.out.println("Exiting Compensate Payment");
        return Response.ok(ParticipantStatus.Compensated.name()).build();
    }

}
