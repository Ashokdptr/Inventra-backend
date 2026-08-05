package com.inventra.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AuditLog> findByModuleOrderByCreatedAtDesc(String module, Pageable pageable);

    Page<AuditLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);

    Page<AuditLog> findBySeverityOrderByCreatedAtDesc(String severity, Pageable pageable);

    Page<AuditLog> findByPerformedByContainingIgnoreCaseOrderByCreatedAtDesc(String user, Pageable pageable);

    Page<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to, Pageable pageable);

    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:module    IS NULL OR a.module    = :module)
          AND (:severity  IS NULL OR a.severity  = :severity)
          AND (:keyword   IS NULL OR LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                  OR LOWER(a.performedBy)  LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:fromDate  IS NULL OR a.createdAt >= :fromDate)
          AND (:toDate    IS NULL OR a.createdAt <= :toDate)
        ORDER BY a.createdAt DESC
    """)
    Page<AuditLog> search(
        @Param("module")   String module,
        @Param("severity") String severity,
        @Param("keyword")  String keyword,
        @Param("fromDate") LocalDateTime fromDate,
        @Param("toDate")   LocalDateTime toDate,
        Pageable pageable
    );

    /** Summary counts per module for stats strip */
    @Query("SELECT a.module, COUNT(a) FROM AuditLog a GROUP BY a.module ORDER BY COUNT(a) DESC")
    List<Object[]> countByModule();

    /** Recent activity — last 50 entries */
    List<AuditLog> findTop50ByOrderByCreatedAtDesc();

    long countByCreatedAtAfter(LocalDateTime after);
}
