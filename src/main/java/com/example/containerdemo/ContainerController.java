package com.example.containerdemo;

import com.github.dockerjava.api.model.Container;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/containers")
public class ContainerController {


    @GetMapping("/ping")
    public Void ping() {
        return ContainerAgent.ping();
    }

    @GetMapping()
    public List<Container> getContainers() {
        return ContainerAgent.getContainers();
    }

    @GetMapping("/{name}")
    public List<Container> getContainer(@PathVariable String name) {
        return ContainerAgent.getContainers(name);
    }

    @PostMapping("/{name}")
    public Container restartContainer(@PathVariable String name) {
        return ContainerAgent.restartContainer(name);
    }
}
