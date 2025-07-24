package com.example.containerdemo.agent;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.WebSocketClient;
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

    public ConcurrentLinkedQueue<ComfyUiEvent> listen(String clientId) {
        ConcurrentLinkedQueue<ComfyUiEvent> sink = new ConcurrentLinkedQueue<>();

        URI uri = baseUri.resolve("ws?clientId=" + clientId);
        Thread.ofVirtual()
                .name("comfy-ui-websocket-client#" + clientId)
                .start(() -> client
                        .execute(uri, session -> {
                            log.info("Connected to WebSocket at: {}", uri);
                            return session.receive()
                                    .map(ComfyUiWebsocketClient::parse)
                                    .takeUntil(this::isFinished)
                                    .doOnNext(event -> {
                                        log.info("Received event: {}", event);
                                        sink.add(event);
                                    })
                                    .log()
                                    .doOnError(error -> log
                                            .error("WebSocket error: ", error))
                                    .doOnComplete(() -> session.close().subscribe())
                                    .doFinally(signal -> log
                                            .info("WebSocket session closed with signal: {}",
                                                    signal))
                                    .subscribeOn(Schedulers.immediate())
                                    .then();
                        }).subscribe());

        return sink;

    }
}
