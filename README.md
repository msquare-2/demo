## Goal

The goal of this exercise is to create a sample application that pulls vendor information and expenses/payments/bills associated with these vendors from Expensify, a third-party system. The application will store this data in MongoDB, retrieving vendor info and transactions on a periodic basis.

## Prerequisites

* Java 17
* Docker
* Docker Compose
* Maven
* Expensify Account with ClientID and ClientSecret

## Running the Application

1. **Build the Application:**`mvn clean install`
2. Change `expensify.client.id` and `expensify.client.secret` inside application.properties
3. **Run Docker Compose:**`docker-compose up --build`
4. **Access the Application:**
   1. **The application will be available at**`http://localhost:8080`.
   2. **The MongoDB can be access on port**`27017`
