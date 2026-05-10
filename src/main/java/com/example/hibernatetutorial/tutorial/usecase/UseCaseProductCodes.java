package com.example.hibernatetutorial.tutorial.usecase;

import java.math.BigDecimal;

final class UseCaseProductCodes {

    static final String LAPTOP_PRODUCT_CODE = "CODE-LAPTOP-PRO";
    static final String PHONE_PRODUCT_CODE = "CODE-PHONE-PLUS";
    static final String HEADSET_PRODUCT_CODE = "CODE-HEADSET-BT";
    static final String KEYBOARD_PRODUCT_CODE = "CODE-KEYBOARD-MECH";
    static final String MOUSE_PRODUCT_CODE = "CODE-MOUSE-WIRELESS";

    static final String LAPTOP_NAME_AFTER_FIND_BY_ID_DEMO = "Ultrabook modifie via findById";
    static final String LAPTOP_NAME_AFTER_REPOSITORY_CACHE_DEMO = "Ultrabook renomme dans le cache";

    static final BigDecimal LAPTOP_INITIAL_PRICE = new BigDecimal("1299.00");
    static final BigDecimal PHONE_INITIAL_PRICE = new BigDecimal("899.00");
    static final BigDecimal HEADSET_INITIAL_PRICE = new BigDecimal("149.90");
    static final BigDecimal KEYBOARD_INITIAL_PRICE = new BigDecimal("119.90");
    static final BigDecimal MOUSE_INITIAL_PRICE = new BigDecimal("49.90");

    private UseCaseProductCodes() {
    }
}
