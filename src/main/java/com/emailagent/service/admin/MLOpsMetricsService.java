package com.emailagent.service.admin;

import com.emailagent.domain.enums.TrainingJobStatus;
import com.emailagent.repository.TrainingJobRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MLOpsMetricsService {

    private final MeterRegistry registry;
    private final TrainingJobRepository trainingJobRepository;

    public MLOpsMetricsService(MeterRegistry registry, TrainingJobRepository trainingJobRepository) {
        this.registry = registry;
        this.trainingJobRepository = trainingJobRepository;
        registerGauges();
    }

    private void registerGauges() {
        Gauge.builder("training_job_total", trainingJobRepository,
                repo -> repo.countByStatus(TrainingJobStatus.QUEUED))
                .tag("status", "QUEUED")
                .description("Training job count by status")
                .register(registry);

        Gauge.builder("training_job_total", trainingJobRepository,
                repo -> repo.countByStatus(TrainingJobStatus.RUNNING))
                .tag("status", "RUNNING")
                .description("Training job count by status")
                .register(registry);

        Gauge.builder("training_job_total", trainingJobRepository,
                repo -> repo.countByStatus(TrainingJobStatus.COMPLETED))
                .tag("status", "COMPLETED")
                .description("Training job count by status")
                .register(registry);

        Gauge.builder("training_job_total", trainingJobRepository,
                repo -> repo.countByStatus(TrainingJobStatus.FAILED))
                .tag("status", "FAILED")
                .description("Training job count by status")
                .register(registry);
    }
}
