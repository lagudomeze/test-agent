package com.example.containerdemo;

public interface AgentTaskBuilder {
    /**
     * Builds an AgentTask instance.
     *
     * @return a new AgentTask instance
     */
    AgentTask build(ContainerAgent agent);
}
