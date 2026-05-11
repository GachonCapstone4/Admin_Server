package com.emailagent.controller.admin;

import com.emailagent.dto.response.admin.operation.AdminDlqCountResponse;
import com.emailagent.dto.response.admin.operation.AdminDlqPurgeResponse;
import com.emailagent.service.admin.RabbitMQManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController
@RequestMapping("/api/admin/rabbitmq")
@RequiredArgsConstructor
public class AdminRabbitMQController {

    private final RabbitMQManagementService rabbitMQManagementService;

    @GetMapping("/dlq/count")
    public AdminDlqCountResponse getDlqCount() {
        int count = rabbitMQManagementService.getDlqMessageCount();
        return new AdminDlqCountResponse(count);
    }

    @DeleteMapping("/dlq/purge")
    public AdminDlqPurgeResponse purgeDlq() {
        log.info("[AdminOperation] DLQ purge 요청 — 관리자");
        rabbitMQManagementService.purgeDlqQueue();
        String purgedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return new AdminDlqPurgeResponse("DLQ purge 완료", purgedAt);
    }
}