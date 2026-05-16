package com.emailagent.dto.response.admin.training;

import com.emailagent.domain.entity.TrainingJob;
import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class TrainingJobListResponse extends BaseResponse {

    private final List<Item> jobs;

    public TrainingJobListResponse(List<Item> jobs) {
        this.jobs = jobs;
    }

    public record Item(
            @JsonProperty("job_id")        String jobId,
            @JsonProperty("job_type")      String jobType,
                                           String status,
            @JsonProperty("error_message") String errorMessage
    ) {
        public static Item from(TrainingJob job) {
            return new Item(
                    job.getJobId(),
                    job.getJobType(),
                    job.getStatus().name(),
                    job.getErrorMessage()
            );
        }
    }
}
