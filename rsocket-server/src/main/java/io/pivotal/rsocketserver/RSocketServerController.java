package io.pivotal.rsocketserver;

import io.pivotal.rsocketserver.data.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Controller
public class RSocketServerController {

    private static final String ORIGIN = "Server";
    private static final String RR = "Request-Response";
    private static final String STREAM = "Stream";
    private static final String CHANNEL = "Channel";

    /**
     * This @MessageMapping is intended to be used "request --> response" style.
     * For each command received, a simple response is generated showing the command sent.
     * @param request
     * @return
     */
    @MessageMapping("command")
    Mono<Message> requestResponse(Message request) {
        log.info("Received request-response request: {}", request);
        // create a Mono containing a single Message and return it
        return Mono.just(new Message(ORIGIN, RR));
    }

    /**
     * This @MessageMapping is intended to be used "fire --> forget" style.
     * When a new CommandRequest is received, a new mono is returned which is empty.
     * @param request
     * @return
     */
    @MessageMapping("notify")
    public Mono<Void> fireAndForget(Message request) {
        log.info("Received fire-and-forget request: {}", request);
        // create an empty (Void) Mono and return it
        return Mono.empty();
    }

    /**
     * This @MessageMapping is intended to be used "subscribe --> stream" style.
     * When a new request command is received, a new stream of events is started and returned to the client.
     * @param request
     * @return
     */
    @MessageMapping("stream")
    Flux<Message> stream(Message request) {
        log.info("Received stream request: {}", request);
        return Flux
                // create a new Flux emitting an element every 1 second
                .interval(Duration.ofSeconds(1))
                // index the Flux
                .index()
                // create a Flux of new Messages using the indexed Flux
                .map(objects -> new Message(ORIGIN, STREAM, objects.getT1()))
                // use the Flux logger to output each flux event
                .log();
    }

    /**
     * This @MessageMapping is intended to be used "stream --> stream" style.
     * When a new stream of CommandRequests is received, a new stream of Messages is started and returned to the client.
     * @param requests
     * @return
     */
    @MessageMapping("channel")
    Flux<Message> channel(Flux<Message> requests) {
        log.info("Received channel request (stream) at {}", Instant.now());
        return requests
                // Create an indexed flux which gives each element a number
                .index()
                // log what has been received
                .log()
                // then every 1 second per element received
                .delayElements(Duration.ofSeconds(1))
                // create a new Flux with one Message for each element (numbered)
                .map(objects -> new Message(ORIGIN, CHANNEL, objects.getT1()))
                // log what is being sent
                .log();
    }
}
