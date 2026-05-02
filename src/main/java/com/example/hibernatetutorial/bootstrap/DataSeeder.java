package com.example.hibernatetutorial.bootstrap;

import com.example.hibernatetutorial.domain.Customer;
import com.example.hibernatetutorial.domain.OrderStatus;
import com.example.hibernatetutorial.domain.Product;
import com.example.hibernatetutorial.domain.PurchaseOrder;
import com.example.hibernatetutorial.repository.CustomerRepository;
import com.example.hibernatetutorial.repository.ProductRepository;
import com.example.hibernatetutorial.repository.PurchaseOrderRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class DataSeeder {

    private final EntityManager entityManager;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public DataSeeder(
            EntityManager entityManager,
            CustomerRepository customerRepository,
            ProductRepository productRepository,
            PurchaseOrderRepository purchaseOrderRepository
    ) {
        this.entityManager = entityManager;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
    }

    @Transactional
    public void resetAndLoad() {
        entityManager.createQuery("delete from OrderLine").executeUpdate();
        purchaseOrderRepository.deleteAllInBatch();
        customerRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();

        Product laptop = new Product("SKU-LAPTOP-PRO", "Ultrabook 14 pouces", new BigDecimal("1299.00"), 25);
        Product phone = new Product("SKU-PHONE-PLUS", "Smartphone Plus", new BigDecimal("899.00"), 40);
        Product headset = new Product("SKU-HEADSET-BT", "Casque Bluetooth", new BigDecimal("149.90"), 80);
        Product keyboard = new Product("SKU-KEYBOARD-MECH", "Clavier mecanique", new BigDecimal("119.90"), 55);
        Product mouse = new Product("SKU-MOUSE-WIRELESS", "Souris sans fil", new BigDecimal("49.90"), 120);

        productRepository.saveAll(List.of(laptop, phone, headset, keyboard, mouse));

        Customer alice = new Customer("Alice", "Martin", "alice.martin@example.com");
        Customer nora = new Customer("Nora", "Bernard", "nora.bernard@example.com");
        Customer yann = new Customer("Yann", "Petit", "yann.petit@example.com");

        customerRepository.saveAll(List.of(alice, nora, yann));

        PurchaseOrder order1 = new PurchaseOrder(alice, OrderStatus.PAID, Instant.now().minus(5, ChronoUnit.DAYS));
        order1.addLine(laptop, 1);
        order1.addLine(mouse, 2);

        PurchaseOrder order2 = new PurchaseOrder(nora, OrderStatus.SHIPPED, Instant.now().minus(3, ChronoUnit.DAYS));
        order2.addLine(phone, 1);
        order2.addLine(headset, 2);

        PurchaseOrder order3 = new PurchaseOrder(yann, OrderStatus.CREATED, Instant.now().minus(1, ChronoUnit.DAYS));
        order3.addLine(keyboard, 2);
        order3.addLine(mouse, 1);
        order3.addLine(headset, 1);

        purchaseOrderRepository.saveAll(List.of(order1, order2, order3));

        // On force l'ecriture puis on vide le contexte pour que les demonstrations commencent sans entites deja managees.
        entityManager.flush();
        entityManager.clear();
    }
}
