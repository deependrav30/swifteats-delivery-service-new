package com.swifteats.simulator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/simulator")
public class SimulatorController {
    private static final Logger log = LoggerFactory.getLogger(SimulatorController.class);
    private final SimulatorService service;

    public SimulatorController(SimulatorService service) {
        this.service = service;
    }

    @PostMapping("/start")
    public ResponseEntity<String> start(@RequestBody SimConfig config) {
        service.start(config);
        return ResponseEntity.accepted().body("started");
    }

    @PostMapping("/stop")
    public ResponseEntity<String> stop() {
        service.stop();
        return ResponseEntity.ok("stopped");
    }
}
