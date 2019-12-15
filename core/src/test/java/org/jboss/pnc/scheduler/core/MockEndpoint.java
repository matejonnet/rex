package org.jboss.pnc.scheduler.core;

import org.jboss.msc.service.ServiceName;
import org.jboss.pnc.scheduler.core.api.TaskContainer;
import org.jboss.pnc.scheduler.common.exceptions.ConcurrentUpdateException;
import org.jboss.pnc.scheduler.common.exceptions.RetryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.transaction.*;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Path("/test")
@Consumes(MediaType.APPLICATION_JSON)
public class MockEndpoint {
    Logger logger = LoggerFactory.getLogger(MockEndpoint.class);

    @Inject
    TaskContainer container;

    ExecutorService executor = Executors.newFixedThreadPool(4);

    @Inject
    TransactionManager tm;

    @POST
    @Path("/accept")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response accept(String request){
        logger.debug("Mock 'accept' endpoint received a request: " + request);
        return Response.ok().build();
    }

    @POST
    @Path("/stop")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response stop(String request){
        logger.debug("Mock 'stop' endpoint received a request: " + request);
        return Response.ok().build();
    }

    @POST
    @Path("/acceptAndStart")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response acceptAndStart(String request){
        logger.debug("Mock 'acceptAndStart' endpoint received a request: " + request);
        executor.submit(() -> retry(10, () -> invokeAccept(request)));
        return Response.ok().build();
    }

    private void invokeAccept(String request) {
        System.out.println("Calling accept on: " + request);
        try {
            tm.begin();
            //parse name out of request and call accept
            container.getTaskController(ServiceName.parse(request.substring(request.indexOf("payload") + 10, request.length()-2))).accept();
            tm.commit();
        }catch (IllegalStateException e) {
            try {
                tm.rollback();
            } catch (SystemException ex) {
                ex.printStackTrace();
            }
        } catch (RollbackException | HeuristicRollbackException | NotSupportedException | HeuristicMixedException | SystemException e) {
            throw new ConcurrentUpdateException("Unexpected error has during committing", e);
        }
    }

    private static void retry(int times, Runnable runnable) {
        for (int i = 0; i < times; i++) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                //shouldn't happen
            }
            try {
                runnable.run();
                break;
            } catch (ConcurrentUpdateException e) {
                System.out.println("Retry number: " + i);
                if (i > 5)
                    e.printStackTrace();
            }
        }
        throw new RetryException("Retrying didn't make effect.");
    }
}
