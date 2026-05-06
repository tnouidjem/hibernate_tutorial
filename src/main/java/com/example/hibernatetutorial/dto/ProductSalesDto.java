package com.example.hibernatetutorial.dto;

import java.math.BigDecimal;

public record ProductSalesDto(
        String productCode,
        String name,
        Long quantitySold,
        BigDecimal revenue
) {
}
