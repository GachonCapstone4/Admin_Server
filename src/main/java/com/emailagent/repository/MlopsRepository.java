package com.emailagent.repository;

import com.emailagent.domain.entity.TrainingJob;
import com.emailagent.domain.enums.TrainingJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MlopsRepository extends JpaRepository<TrainingJob, String> {
    long countByStatus(TrainingJobStatus status);
    List<TrainingJob> findAllByOrderByCreatedAtDesc();
}
