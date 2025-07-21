package com.example.containerdemo;

import java.time.Duration;

public interface AgentTask extends Runnable {
    String id();

    Duration timeout();

    boolean tryCancel();
}
