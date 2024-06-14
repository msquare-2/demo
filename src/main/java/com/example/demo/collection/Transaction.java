package com.example.demo.collection;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "transactions")
public class Transaction {
    @Id
    private String id;
    private String merchant;
    private Integer amount;
    private String category;
    @DBRef
    private Receipt receipt;

}
