package com.example.postscheduler.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables @Scheduled jobs (used to purge expired refresh tokens). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
