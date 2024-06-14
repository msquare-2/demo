package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.example.demo.collection.Receipt;
import com.example.demo.collection.Report;
import com.example.demo.collection.Transaction;
import com.example.demo.config.ExpensifyConfig;
import com.example.demo.repository.RecieptRepository;
import com.example.demo.repository.ReportRepository;
import com.example.demo.repository.TransactionRepository;
import com.example.demo.utils.TemplateUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RecieptRepository receiptRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ExpensifyConfig expensifyConfig;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String EXPENSIFY_API_URL = "https://integrations.expensify.com/Integration-Server/ExpensifyIntegrations";

    public void fetchAndSaveReports() {
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        MultiValueMap<String, String> requestHeaders = new LinkedMultiValueMap<>();
        HttpHeaders headers = new HttpHeaders();

        Map<String, Object> reportGenerationReqPayload = new HashMap<>();
        Map<String, Object> reportDownloadReqPayload = new HashMap<>();
        try {

            requestBody.add("requestJobDescription", getReportGenerationJobDescription());
            requestBody.add("template", TemplateUtils.loadTemplate("classpath:templates/expensify_template.ftl"));

            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> responseEntity = restTemplate.exchange(EXPENSIFY_API_URL, HttpMethod.POST,
                    requestEntity, String.class);

            if (responseEntity.getStatusCode().equals(HttpStatus.OK)) {
                String reportName = responseEntity.getBody();
                reportDownloadReqPayload.put("requestJobDescription", Map.of(
                        "type", "download",
                        "credentials", Map.of(
                                "partnerUserID", expensifyConfig.getId(),
                                "partnerUserSecret", expensifyConfig.getSecret()),
                        "fileName", reportName,
                        "fileSystem", "integrationServer"));
                String report = restTemplate.postForObject(EXPENSIFY_API_URL, reportDownloadReqPayload, String.class);
                System.out.println(report);
            } else {
                System.out.println(responseEntity.getStatusCode());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        // Fetch data from external API
        Report[] reports = restTemplate.getForObject(EXPENSIFY_API_URL, Report[].class);

        if (reports != null) {
            // Process and save the data
            saveReports(Arrays.asList(reports));
        }
    }

    private String getReportGenerationJobDescription() throws JsonProcessingException {
        Map<String, Object> requestJobDescription = new HashMap<>();
        requestJobDescription.put("type", "file");
        requestJobDescription.put("credentials", Map.of(
                "partnerUserID", expensifyConfig.getId(),
                "partnerUserSecret", expensifyConfig.getSecret()));
        requestJobDescription.put("onReceive", Map.of(
                "immediateResponse", List.of("returnRandomFileName")));
        requestJobDescription.put("inputSettings", Map.of(
                "type", "combinedReportData",
                "reportState", "APPROVED,REIMBURSED",
                "limit", "10",
                "filters", Map.of(
                        "startDate", "2024-06-11",
                        "endDate", "2024-06-14",
                        "markedAsExported", "Expensify Export")));
        requestJobDescription.put("outputSettings", Map.of(
                "fileExtension", "json",
                "fileBasename", "myExport"));
        requestJobDescription.put("onFinish", List.of(
                Map.of("actionName", "markAsExported", "label", "Expensify Export"),
                Map.of("actionName", "email", "recipients", "manager@domain.com,finances@domain.com", "message",
                        "Report is ready.")));
        return objectMapper.writeValueAsString(requestJobDescription);
    }

    public void saveReports(List<Report> reports) {
        for (Report report : reports) {
            // Save transactions with nested receipt objects
            List<Transaction> transactions = report.getTransactionList().stream().map(transaction -> {
                // Save receipt
                Receipt receipt = transaction.getReceipt();
                if (receipt != null) {
                    receipt.setId(UUID.randomUUID().toString());
                    receiptRepository.save(receipt);
                }
                // Save transaction
                transaction.setId(UUID.randomUUID().toString());
                transactionRepository.save(transaction);
                return transaction;
            }).collect(Collectors.toList());

            // Set the saved transactions to the report
            report.setTransactionList(transactions);

            // Save report
            reportRepository.save(report);
        }
    }
}
