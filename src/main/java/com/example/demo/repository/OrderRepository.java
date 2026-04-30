package com.example.demo.repository;

import com.example.demo.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    List<Order> findByStatus(Order.OrderStatus status);
    List<Order> findByStatusIn(List<Order.OrderStatus> statuses);

    @Query("SELECT o FROM Order o JOIN o.user u WHERE " +
           "o.status NOT IN (com.example.demo.model.Order.OrderStatus.COTIZACION, com.example.demo.model.Order.OrderStatus.IGNORED) AND " +
           "(:searchTerm IS NULL OR " +
           "CAST(o.id AS string) LIKE :searchTerm OR " +
           "LOWER(u.firstName) LIKE :searchTerm OR " +
           "LOWER(u.lastName) LIKE :searchTerm OR " +
           "LOWER(u.email) LIKE :searchTerm) AND " +
           "(CAST(:startDate AS timestamp) IS NULL OR o.createdAt >= :startDate) AND " +
           "(CAST(:endDate AS timestamp) IS NULL OR o.createdAt <= :endDate)")
    Page<Order> searchOrders(@Param("searchTerm") String searchTerm, 
                            @Param("startDate") LocalDateTime startDate, 
                            @Param("endDate") LocalDateTime endDate, 
                            Pageable pageable);

    @Query("SELECT CAST(o.createdAt AS date), SUM(o.total) " +
           "FROM Order o " +
           "WHERE o.createdAt >= :startDate " +
           "AND o.status NOT IN (com.example.demo.model.Order.OrderStatus.COTIZACION, com.example.demo.model.Order.OrderStatus.IGNORED) " +
           "GROUP BY CAST(o.createdAt AS date) " +
           "ORDER BY CAST(o.createdAt AS date) ASC")
    List<Object[]> getSalesStatsByDay(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status NOT IN (com.example.demo.model.Order.OrderStatus.COTIZACION, com.example.demo.model.Order.OrderStatus.IGNORED)")
    long countConfirmedOrders();

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.status NOT IN (com.example.demo.model.Order.OrderStatus.COTIZACION, com.example.demo.model.Order.OrderStatus.IGNORED)")
    BigDecimal sumConfirmedRevenue();
}
