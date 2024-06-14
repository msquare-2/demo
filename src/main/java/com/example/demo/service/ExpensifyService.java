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
public class ExpensifyService {

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
        MultiValueMap<String, String> reportGenerationRequestBody = new LinkedMultiValueMap<>();
        HttpHeaders reportGenerationHeaders = new HttpHeaders();

        try {

            reportGenerationRequestBody.add("requestJobDescription", getReportGenerationJobDescription());
            reportGenerationRequestBody.add("template",
                    TemplateUtils.loadTemplate("classpath:templates/expensify_template.ftl"));

            reportGenerationHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> reportGenerationRequestEntity = new HttpEntity<>(
                    reportGenerationRequestBody, reportGenerationHeaders);

            ResponseEntity<String> reportGenerationResponseEntity = restTemplate.exchange(EXPENSIFY_API_URL,
                    HttpMethod.POST,
                    reportGenerationRequestEntity, String.class);

            if (reportGenerationResponseEntity.getStatusCode().equals(HttpStatus.OK)) {
                Report[] reports = fetchDownloadReport(reportGenerationResponseEntity.getBody());
                if (reports != null && reports.length > 0) {
                    saveReports(Arrays.asList(reports));
                }
            } else {
                System.out.println(reportGenerationResponseEntity.getStatusCode());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        // Fetch data from external API
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

    private Report[] fetchDownloadReport(String reportFileName) {
        MultiValueMap<String, String> reportDownloadRequestBody = new LinkedMultiValueMap<>();
        HttpHeaders reportDownloadHeaders = new HttpHeaders();
        reportDownloadHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        Map<String, Object> requestJobDescriptionMap = new HashMap<>();
        requestJobDescriptionMap.put("type", "download");
        requestJobDescriptionMap.put("credentials", Map.of(
                "partnerUserID", expensifyConfig.getId(),
                "partnerUserSecret", expensifyConfig.getSecret()));
        requestJobDescriptionMap.put("fileName", reportFileName);
        requestJobDescriptionMap.put("fileSystem", "integrationServer");
        
        try {
            String requestJobDescription = objectMapper.writeValueAsString(requestJobDescriptionMap);

            reportDownloadRequestBody.add("requestJobDescription", requestJobDescription);

            HttpEntity<MultiValueMap<String, String>> reportDownloadRequestEntity = new HttpEntity<>(
                    reportDownloadRequestBody, reportDownloadHeaders);

            ResponseEntity<String> reportDownloadresponseEntity = restTemplate.exchange(EXPENSIFY_API_URL,
                    HttpMethod.POST,
                    reportDownloadRequestEntity, String.class);

            if(reportDownloadresponseEntity.getBody() != null) {
                Report[] reports = objectMapper.readValue(reportDownloadresponseEntity.getBody(), Report[].class);
                return reports;
            }
            System.out.println(reportDownloadresponseEntity.getBody());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
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
