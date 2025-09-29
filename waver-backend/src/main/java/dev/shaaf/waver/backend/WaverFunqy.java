package dev.shaaf.waver.backend;

import io.quarkus.funqy.Funq;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Serverless function endpoint for handling tutorial generation requests.
 * <p>
 * This class provides a Funqy endpoint that can be invoked via HTTP requests
 * or cloud events. It acts as the entry point for external systems to trigger
 * tutorial generation processes.
 * <p>
 * The function validates incoming requests and forwards them to the background
 * processing system through reactive messaging.
 *
 * @author Waver Team
 * @version 1.0
 * @since 1.0
 */
@ApplicationScoped
public class WaverFunqy {

    /**
     * Message emitter for sending processing requests to the background service.
     * This emitter is connected to the "requests" channel for async processing.
     */
    @Inject
    @Channel("requests")
    Emitter<WaverProcessEvent> requestEmitter;

    /**
     * Funqy endpoint for generating tutorials from source code repositories.
     * <p>
     * This function validates the incoming request and forwards it to the
     * background processing system. It provides immediate response while
     * the actual tutorial generation happens asynchronously.
     *
     * @param request The processing event containing the source URL
     *               and other generation parameters
     */
    @Funq
    public void generate(WaverProcessEvent request) {

        if (request == null || request.sourceUrl() == null) {
            System.err.println("Received invalid request: payload or sourceUrl is null.");
            return;
        }
        
        System.out.println("FUNQY_ENDPOINT: Received request for " + request.sourceUrl() + ". Handing off to background processor.");

        // call back immediately and forward
        requestEmitter.send(request);
    }
}