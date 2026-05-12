package com.emailagent;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;

/**
 * Admin 전용 경량 서버 진입점.
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
                "com.emailagent.rabbitmq.config",     // RabbitMQConfig (q.2app.training 큐 설정)
                "com.emailagent.rabbitmq.consumer",  // TrainingResultConsumer
                "com.emailagent.rabbitmq.publisher.admin", // AdminPublisher (모델 배포 요청 발행)
                "com.emailagent.rabbitmq.event"      // SseFanoutPublisher
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
