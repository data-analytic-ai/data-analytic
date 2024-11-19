package ai.dataanalytic.databridge.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {


    @GetMapping("/health")
    public String healthCheck() {
        return "Data Bridge is running!";
    }
}
