package com.example.hibernatetutorial.repository;

import com.example.hibernatetutorial.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
