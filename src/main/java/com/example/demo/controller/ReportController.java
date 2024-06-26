package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.ExpensifyService;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ExpensifyService reportService;

    @GetMapping("/fetch")
    public void fetchAndSaveReports() {
        reportService.fetchAndSaveReports();
    }
}
