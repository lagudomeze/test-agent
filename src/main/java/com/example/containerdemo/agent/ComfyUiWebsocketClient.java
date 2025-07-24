package com.example.containerdemo.agent;

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

    public ConcurrentLinkedQueue<ComfyUiEvent> listen(String clientId, Duration timeout) {
        ConcurrentLinkedQueue<ComfyUiEvent> sink = new ConcurrentLinkedQueue<>();

        URI uri = baseUri.resolve("ws?clientId=" + clientId);
        Thread.ofVirtual()
                .name("comfy-ui-websocket-client#" + clientId)
                .start(() -> client
                        .execute(uri, session -> {
                            log.info("Connected to WebSocket at: {}", uri);
                            return session
                                    .receive()
                                    .map(ComfyUiWebsocketClient::parse)
                                    .takeUntil(this::isFinished)
                                    .doOnNext(event -> {
                                        log.info("Received event: {}", event);
                                        sink.add(event);
                                    })
                                    .doOnComplete(() -> session.close().subscribe())
                                    .then()
                                    .subscribeOn(Schedulers.immediate())
                                    .timeout(timeout)
                                    .onErrorResume(TimeoutException.class, e -> {
                                        log.warn("WebSocket connection timed out", e);
                                        return Mono.empty();
                                    });
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
                })
                .doOnNext(i -> log.info("Received: {}", i));

        Mono<Void> task = source.takeUntil(i -> i >= 30)
                .doOnNext(i -> log.info("haha: {}", i))
                .doOnComplete(() -> log.info("done"))
                .then();

        task.timeout(Duration.ofSeconds(3))
                .doOnError(error -> log.warn("error", error))
                .onErrorResume(TimeoutException.class, e -> {
                    log.warn("timeout", e);
                    return Mono.empty();
                })
                .subscribeOn(Schedulers.immediate())
                .subscribe();


    }
}
