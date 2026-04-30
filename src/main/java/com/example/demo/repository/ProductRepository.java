package com.example.demo.repository;

import com.example.demo.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByActive(Boolean active);
    List<Product> findByCategoryId(Long categoryId);
    Product findBySku(Long sku);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.stock <= p.minStock AND p.active = true")
    List<Product> findLowStockProducts();
    
    @org.springframework.data.jpa.repository.Query(value = "SELECT COALESCE(MAX(sku), 9999) + 1 FROM products", nativeQuery = true)
    Long findNextSku();
}
