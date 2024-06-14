package com.example.demo.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.demo.collection.Report;

public interface ReportRepository extends MongoRepository<Report, String> {
}
