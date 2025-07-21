package com.example.containerdemo;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.time.Duration;
import java.util.List;

@Slf4j
public record ContainerAgent(String name, URI baseUrl) {

    private static final DockerClientConfig config = DefaultDockerClientConfig
            .createDefaultConfigBuilder()
            .build();

    private static final DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .maxConnections(100)
            .connectionTimeout(Duration.ofSeconds(30))
            .responseTimeout(Duration.ofSeconds(45))
            .build();

    private static final DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);

    public static Void ping() {
        return dockerClient.pingCmd().exec();
    }

    public static List<Container> getContainers() {
        return dockerClient
                .listContainersCmd()
                .withShowAll(true)
                .exec();
    }

    public static List<Container> getContainers(String name) {
        return dockerClient
                .listContainersCmd()
                .withNameFilter(List.of(name))
                .withShowAll(true)
                .exec();
    }

    public static Container restartContainer(String name) {
        List<Container> containers = getContainers(name);
        if (containers.isEmpty()) {
            throw new IllegalArgumentException("Container with name '" + name + "' not found.");
        }
        if (containers.size() > 1) {
            throw new IllegalArgumentException("Multiple containers found with name '" + name + "'. Please specify a unique name.");
        }
        Container target = containers.getFirst();

        dockerClient
                .restartContainerCmd(target.getId())
                .exec();

        return getContainers().getFirst();
    }

    public boolean available() {
        // todo
        return true;
    }

    public void restart() {
        log.warn("Restarting container: {}", name);
        // todo
    }
}
