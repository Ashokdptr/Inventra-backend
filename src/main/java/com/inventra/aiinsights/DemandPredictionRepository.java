package com.inventra.aiinsights;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemandPredictionRepository extends JpaRepository<DemandPrediction, Long> {

    @Query("SELECT d FROM DemandPrediction d JOIN FETCH d.product ORDER BY d.predictionDate DESC")
    List<DemandPrediction> findAllWithProduct();
}
