package com.example.containerdemo;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;

@Slf4j
public class AgentScheduler implements AutoCloseable {
    private static final ExecutorService service = Executors.newVirtualThreadPerTaskExecutor();

    @Slf4j
    public record AgentTaskRunner(ContainerAgent agent) {

        public void run(AgentTaskBuilder builder) {
            try {
                if (!agent.available()) {
                    agent.restart();
                }

                AgentTask task = builder.build(agent);

                Thread thread = Thread.ofVirtual()
                        .name("agent(" + agent.name() + ")-task(" + task.id() + ")")
                        .start(task);

                if (!thread.join(task.timeout())) {
                    log.info("Agent task timed out.");
                    if (task.tryCancel()) {
                        log.info("Task {} cancelled successfully.", task);
                    } else {
                        log.warn("Failed to cancel task {}. restart agent", task);
                        agent.restart();
                    }
                }
            } catch (InterruptedException e) {
                log.error("Thread interrupted while running task in agent: {}", agent.name(), e);
                Thread.currentThread().interrupt(); // Restore the interrupted status
            } catch (Exception e) {
                log.error("Error occurred while processing task in agent: {}", agent.name(), e);
                // Optionally, you can add logic to handle task completion or cleanup here
            }
        }
    }

    public void init(List<ContainerAgent> agents) {
        for (ContainerAgent agent : agents) {
            waitingAgentTaskRunners.add(new AgentTaskRunner(agent));
        }
    }

    private final LinkedTransferQueue<AgentTaskRunner> waitingAgentTaskRunners = new LinkedTransferQueue<>();

    @Override
    public void close() {
        service.shutdown();
    }

    public void run(AgentTaskBuilder builder) {
        try {
            AgentTaskRunner runner = waitingAgentTaskRunners.take();
            service.submit(() -> {
                try {
                    runner.run(builder);
                } finally {
                    waitingAgentTaskRunners.add(runner);
                }
            });
        } catch (InterruptedException e) {
            log.error("Failed to take a waiting agent task runner", e);
            Thread.currentThread().interrupt(); // Restore the interrupted status
        }
    }
}
