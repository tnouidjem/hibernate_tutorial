package com.example.hibernatetutorial.repository;

import com.example.hibernatetutorial.domain.Product;
import com.example.hibernatetutorial.dto.ProductSalesDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    @Query("select p.id from Product p where p.sku = :sku")
    Long findIdBySku(@Param("sku") String sku);

    @Query("""
            select p
            from Product p
            where p.price >= :minimumPrice
            order by p.price desc
            """)
    List<Product> findProductsFromPrice(@Param("minimumPrice") BigDecimal minimumPrice);

    @Query("""
            select new com.example.hibernatetutorial.dto.ProductSalesDto(
                p.sku,
                p.name,
                sum(l.quantity),
                sum(l.unitPriceSnapshot * l.quantity)
            )
            from OrderLine l
            join l.product p
            group by p.sku, p.name
            order by sum(l.quantity) desc
            """)
    List<ProductSalesDto> findSalesSummary();
}
