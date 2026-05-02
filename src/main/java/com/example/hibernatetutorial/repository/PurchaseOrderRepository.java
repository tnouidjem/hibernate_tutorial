package com.example.hibernatetutorial.repository;

import com.example.hibernatetutorial.domain.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    Optional<PurchaseOrder> findFirstByOrderByIdAsc();

    @Query("""
            select distinct o
            from PurchaseOrder o
            left join fetch o.lines l
            left join fetch l.product
            where o.id = :id
            """)
    Optional<PurchaseOrder> findWithLinesById(@Param("id") Long id);
}
