package com.emailagent;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;

/**
 * Admin 전용 경량 서버 진입점.
 *
 * [비활성화 전략] Option B: scanBasePackages 제한 + @ConditionalOnProperty 이중 방어
 *
 * [scanBasePackages 제외로 자연히 미로드되는 것]
 * - com.emailagent.service                  → GmailApiService, GoogleOAuthService,
 *                                              GoogleCalendarApiService, GoogleApiClientProvider,
 *                                              PubSubHandlerService, InboxService, CalendarService 등
 * - com.emailagent.controller               → AuthController, IntegrationController, WebhookController 등
 * - com.emailagent.rabbitmq.scheduler       → MailScheduler (Outbox 폴링 스케줄러)
 * - com.emailagent.rabbitmq.publisher       → MailPublisher (Outbox RabbitMQ 발행자)
 * - com.emailagent.rabbitmq.service         → MailService / MailServiceImpl (Outbox 서비스)
 * - com.emailagent.rabbitmq.controller      → MailController
 *
 * [이중 방어 - @ConditionalOnProperty]
 * - MailConsumer: application-admin.yml의 admin.classify-consumer.enabled=false 로 추가 비활성화
 *
 * [실행]
 * mvn spring-boot:run \
 *   -Dspring-boot.run.mainClass=com.emailagent.AdminServerApplication \
 *   -Dspring-boot.run.profiles=admin
 */
@SpringBootApplication(
        scanBasePackages = {
                "com.emailagent.config",            // SecurityConfig
                "com.emailagent.security",          // JwtTokenProvider, JwtAuthenticationFilter, CustomUserDetailsService
                "com.emailagent.controller.admin",  // Admin 컨트롤러만
                "com.emailagent.service.admin",     // Admin 서비스만
                "com.emailagent.repository",        // JPA Repository 전체 (Admin 서비스 직접 의존)
                "com.emailagent.exception",         // GlobalExceptionHandler
                "com.emailagent.rabbitmq.config",   // RabbitMQConfig (q.2app.training 큐 설정)
                "com.emailagent.rabbitmq.consumer", // TrainingResultConsumer (MailConsumer는 비활성화)
                "com.emailagent.rabbitmq.event"     // SseFanoutPublisher
        }
)
@EnableAsync
public class AdminServerApplication {

    @PostConstruct
    public void started() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    public static void main(String[] args) {
        SpringApplication.run(AdminServerApplication.class, args);
        System.out.println("v1 정상시작");
    }
}
