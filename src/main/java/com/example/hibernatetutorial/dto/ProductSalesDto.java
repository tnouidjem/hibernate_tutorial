package com.example.hibernatetutorial.dto;

import java.math.BigDecimal;

public record ProductSalesDto(
        String sku,
        String name,
        Long quantitySold,
        BigDecimal revenue
) {
}
