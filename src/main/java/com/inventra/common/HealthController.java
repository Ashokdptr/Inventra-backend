package com.inventra.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/")
    public String home() {
        return "Inventra Backend Running";
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}