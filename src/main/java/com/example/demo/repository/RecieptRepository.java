package com.example.demo.repository;

import com.example.demo.collection.Receipt;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RecieptRepository extends MongoRepository<Receipt, String> {
}
