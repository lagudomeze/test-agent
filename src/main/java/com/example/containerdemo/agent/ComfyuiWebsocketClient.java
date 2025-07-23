package com.example.containerdemo.agent;

import org.springframework.web.reactive.socket.client.WebSocketClient;

import java.net.URI;

public record ComfyuiWebsocketClient(URI baseUri, WebSocketClient client) {


}
