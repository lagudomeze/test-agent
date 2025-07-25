package com.example.containerdemo.agent;

import com.example.containerdemo.agent.ComfyUiWebsocketClient.ListenEvent.Finished;
import com.example.containerdemo.agent.ComfyUiWebsocketClient.ListenEvent.Ok;
import com.example.containerdemo.agent.ComfyUiWebsocketClient.ListenEvent.Timeout;
import com.example.containerdemo.agent.ComfyUiWebsocketClient.ListenEvent.Unexpected;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
public record ComfyUiWebsocketClient(URI baseUri, WebSocketClient client) {

    private static final JsonMapper mapper = new JsonMapper();

    private static final ObjectReader reader = mapper.readerFor(ComfyUiEvent.class);

    @SneakyThrows
    private static ComfyUiEvent parse(WebSocketMessage message) {
        return reader.readValue(message.getPayloadAsText());
    }

    private boolean isFinished(ComfyUiEvent event) {
        //todo
        return false;
    }

    public sealed interface ListenEvent permits Finished, Ok, Timeout, Unexpected {

        record Finished() implements ListenEvent {

        }

        record Ok(ComfyUiEvent event) implements ListenEvent {

        }

        record Timeout(Duration duration, TimeoutException exception) implements ListenEvent {

        }

        record Unexpected(Throwable exception) implements ListenEvent {

        }
    }

    public ConcurrentLinkedQueue<ListenEvent> listen(String clientId, Duration timeout) {
        ConcurrentLinkedQueue<ListenEvent> sink = new ConcurrentLinkedQueue<>();

        URI uri = baseUri.resolve("ws?clientId=" + clientId);
        Thread.ofVirtual()
                .name("comfy-ui-websocket-client#" + clientId)
                .start(() -> client
                        .execute(uri, session -> {
                            log.info("Connected to websocket at: {}", uri);
                            return session
                                    .receive()
                                    .map(ComfyUiWebsocketClient::parse)
                                    .takeUntil(this::isFinished)
                                    .doOnNext(event -> {
                                        log.info("Received event: {}", event);
                                        sink.add(new Ok(event));
                                    })
                                    .doFinally(signal -> {
                                        log.info("Closing websocket at: {} with signal: {}", uri,
                                                signal);
                                        session.close()
                                                .doOnError(e -> log
                                                        .warn("session close error", e))
                                                .subscribe();
                                    })
                                    .then()
                                    // use current thread for immediate execution
                                    .subscribeOn(Schedulers.immediate())
                                    // set timeout to the entire WebSocket session
                                    .timeout(timeout)
                                    // timeout error handling
                                    .onErrorResume(TimeoutException.class, e -> {
                                        log.warn("WebSocket session timed out after {}",
                                                timeout, e);
                                        sink.add(new Timeout(timeout, e));
                                        return Mono.empty();
                                    })
                                    // other error handling
                                    .onErrorResume(e -> {
                                        log.error("Unexpected error", e);
                                        sink.add(new Unexpected(e));
                                        return Mono.empty();
                                    })
                                    .doOnSuccess(event -> sink.add(new Finished()));
                        })
                        .subscribe()
                );

        return sink;

    }

    public static void main(String[] args) {

        Flux<Integer> source = Flux.range(1, 20)
                .map(i -> i * 2)
                .doOnNext(i -> {
                    log.info("Processing: {}", i);
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    if (i > 100) {
                        throw new RuntimeException();
                    }
                })
                .doOnNext(i -> log.info("Received: {}", i))
                .doFinally(signal -> log.info("Closing websocket with signal: {}", signal));

        Mono<Void> task = source.takeUntil(i -> i >= 30)
                .doOnNext(i -> log.info("haha: {}", i))
                .doOnComplete(() -> log.info("done"))
                .then();

        task.timeout(Duration.ofSeconds(10))
                .onErrorResume(TimeoutException.class, e -> {
                    log.warn("timeout", e);
                    return Mono.empty();
                })
                .onErrorResume(e -> {
                    log.error("Unexpected error", e);
                    return Mono.empty();
                })
                .subscribeOn(Schedulers.immediate())
                .subscribe();


    }
}
