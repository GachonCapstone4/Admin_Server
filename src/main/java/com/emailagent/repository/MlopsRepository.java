package com.emailagent.repository;

import com.emailagent.domain.entity.TrainingJob;
import com.emailagent.domain.enums.TrainingJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MlopsRepository extends JpaRepository<TrainingJob, String> {
    long countByStatus(TrainingJobStatus status);
}
