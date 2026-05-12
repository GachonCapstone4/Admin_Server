package com.emailagent.service.admin;

import com.emailagent.domain.enums.TrainingJobStatus;
import com.emailagent.repository.MlopsRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MLOpsMetricsService {

    private final MeterRegistry registry;
    private final MlopsRepository mlopsRepository;

    public MLOpsMetricsService(MeterRegistry registry, MlopsRepository mlopsRepository) {
        this.registry = registry;
        this.mlopsRepository = mlopsRepository;
        registerGauges();
    }

    private void registerGauges() {
        Gauge.builder("training_job_total", mlopsRepository,
                repo -> repo.countByStatus(TrainingJobStatus.QUEUED))
                .tag("status", "QUEUED")
                .description("Training job count by status")
                .register(registry);

        Gauge.builder("training_job_total", mlopsRepository,
                repo -> repo.countByStatus(TrainingJobStatus.RUNNING))
                .tag("status", "RUNNING")
                .description("Training job count by status")
                .register(registry);

        Gauge.builder("training_job_total", mlopsRepository,
                repo -> repo.countByStatus(TrainingJobStatus.COMPLETED))
                .tag("status", "COMPLETED")
                .description("Training job count by status")
                .register(registry);

        Gauge.builder("training_job_total", mlopsRepository,
                repo -> repo.countByStatus(TrainingJobStatus.FAILED))
                .tag("status", "FAILED")
                .description("Training job count by status")
                .register(registry);
    }
}
