package com.emailagent.service.admin;

import com.emailagent.dto.response.admin.training.TrainingJobListResponse;
import com.emailagent.repository.MlopsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TrainingJobQueryService {

    private final MlopsRepository mlopsRepository;

    @Transactional(readOnly = true)
    public TrainingJobListResponse getJobList() {
        List<TrainingJobListResponse.Item> jobs = mlopsRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(TrainingJobListResponse.Item::from)
                .toList();
        return new TrainingJobListResponse(jobs);
    }
}
