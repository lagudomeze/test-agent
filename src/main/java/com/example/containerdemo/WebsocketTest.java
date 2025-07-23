package com.example.containerdemo;

import org.springframework.web.reactive.socket.client.StandardWebSocketClient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class WebsocketTest {
    private static CountDownLatch messageLatch;
    private static final String SENT_MESSAGE = "Hello World";

    public static void main(String[] args) {

        try {
            messageLatch = new CountDownLatch(1);

            StandardWebSocketClient client = new StandardWebSocketClient();


            messageLatch.await(100, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
