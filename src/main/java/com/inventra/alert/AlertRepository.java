package com.inventra.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    @Query("SELECT a FROM Alert a LEFT JOIN FETCH a.product ORDER BY a.createdAt DESC")
    List<Alert> findAllWithProduct();

    @Query("SELECT a FROM Alert a LEFT JOIN FETCH a.product WHERE a.isRead = false ORDER BY a.createdAt DESC")
    List<Alert> findUnread();

    long countByIsReadFalse();
}
