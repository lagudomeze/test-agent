package com.example.containerdemo.agent;

public interface ComfyUiAgent {

    ComfyUiRestClient restClient();

    ComfyUiWebsocketClient websocketClient();

}
