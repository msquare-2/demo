package com.example.demo.collection;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "reports")
public class Report {
    @Id
    private String reportID;
    private String reportName;
    private String accountEmail;
    private String reportCreated;
    
    private List<Transaction> transactionList;

    // Getters and setters
}
