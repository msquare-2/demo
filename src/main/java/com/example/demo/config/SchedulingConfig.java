package com.example.demo.config;

import com.example.demo.service.ExpensifyService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class SchedulingConfig {

    @Autowired
    private ExpensifyService expensifyService;

    @Scheduled(fixedRate = 86400000) // every 24 hours triggers
    public void scheduleExpensifyDataPull() {
        expensifyService.fetchAndSaveReports();
    }
}
