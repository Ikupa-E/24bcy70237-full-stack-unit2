package com.example.postscheduler.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Demo only: lets you trigger an unexpected server error to test logging. Remove before production. */
@RestController
@RequestMapping("/api/v1/diagnostics")
@PreAuthorize("hasRole('ADMIN')")
public class DiagnosticsController {

    @GetMapping("/error")
    public String triggerError() {
        throw new IllegalArgumentException("Simulated unexpected failure");
    }
}
