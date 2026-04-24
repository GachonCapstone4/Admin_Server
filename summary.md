# Spring Profile 기반 Admin Server 구성 요약

## 배경
Spring Profile을 이용해 Admin 기능만 동작하는 경량 서버를 임시로 구성하는 방법 검토.

---

## 두 접근법 비교

| 기준 | Option A (Admin에 `@Profile("admin")`) | Option B (Non-Admin에 `@Profile("!admin")`) |
|------|--------------------------------------|---------------------------------------------|
| 수정 파일 수 | ~15개 (admin 클래스) | ~28+개 (non-admin 클래스) |
| Admin 격리 효과 | ❌ Non-admin 빈이 여전히 로드됨 | ✅ Non-admin 빈이 완전히 제외됨 |
| 기존 서비스 서버 영향 | ❌ admin profile 없으면 Admin 빈 누락 | ✅ profile 없으면 모든 빈 로드 |
| 목표 달성 여부 | ❌ 목적 달성 불가 | ✅ 목적 달성 |

**Option A의 결정적 문제**: admin profile로 서버를 띄워도 `GmailApiService`, `PubSubHandlerService`, `NotificationService` 등 Non-Admin 빈이 여전히 Spring Context에 로드됨. Google OAuth 환경변수가 없으면 startup fail 가능성 있음.

---

## 권장안: scanBasePackages 제한 방식

이미 패키지가 `controller/admin/`, `service/admin/`으로 분리되어 있으므로, `@Profile`을 28개 파일에 다 달기보다 **컴포넌트 스캔 제한**이 더 간결함.

- 신규 파일: `AdminServerApplication.java` 1개 + `application-admin.yml` 1개
- 기존 코드 수정: 최소화

### AdminServerApplication.java

```java
package com.emailagent;

@SpringBootApplication(
    scanBasePackages = {
        "com.emailagent.config",           // SecurityConfig, RabbitMQConfig
        "com.emailagent.security",         // JWT 인증 레이어
        "com.emailagent.controller.admin", // Admin 컨트롤러만
        "com.emailagent.service.admin",    // Admin 서비스만
        "com.emailagent.repository",       // 전체 Repository (JPA 자동 스캔)
        "com.emailagent.domain",           // Entity
        "com.emailagent.exception",        // GlobalExceptionHandler
        "com.emailagent.converter",
        "com.emailagent.util"
    },
    exclude = {
        RabbitAutoConfiguration.class      // RabbitMQ Consumer 완전 제외 시
    }
)
public class AdminServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminServerApplication.class, args);
    }
}
```

### application-admin.yml

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        auto-startup: false  # RabbitMQ Consumer 비활성화

logging:
  level:
    com.emailagent.rabbitmq.consumer: WARN
```

### RabbitMQ Consumer 선택적 활성화 (기존 파일 수정)

```java
@Component
@ConditionalOnProperty(
    name = "spring.rabbitmq.listener.simple.auto-startup",
    havingValue = "true",
    matchIfMissing = true
)
public class EmailClassifyConsumer { ... }
```

---

## Admin 동작에 필요한 최소 Non-Admin 기능

### 필수 (Admin이 직접 의존)
- `SecurityConfig` — JWT 필터 체인, CORS, 권한 설정
- `JwtTokenProvider` — 토큰 생성/검증
- `JwtAuthenticationFilter` — 요청별 JWT 검증
- `CustomUserDetailsService` — userId 기반 User 로드
- `GlobalExceptionHandler` — 에러 응답 공통 형식
- `ObjectMapper` (Jackson) — JSON 직렬화
- JPA Repository 전체 — Admin 서비스가 직접 호출

### 선택적 (AiTraining 기능 필요 시)
- `RabbitMQConfig` — q.2app.training 큐 수신용
- `TrainingResultConsumer` — 학습 결과 수신 consumer

### 완전히 불필요 (Admin에서 제외)
- `GmailApiService`, `GoogleCalendarApiService`, `GoogleOAuthService`
- `InboxService`, `EmailService`, `DraftService`, `CalendarService`
- `OnboardingService`, `BusinessService`
- `PubSubHandlerService`
- `NotificationService` (SSE 알림)
- `EmailSyncScheduler`
- `FileTextExtractor`

---

## 실행 방법

```bash
# Admin 서버 실행
java -jar app.jar \
  --spring.main.sources=com.emailagent.AdminServerApplication \
  --spring.profiles.active=admin

# Maven으로 실행
mvn spring-boot:run \
  -Dspring-boot.run.mainClass=com.emailagent.AdminServerApplication \
  -Dspring-boot.run.profiles=admin
```

---

## 결론

| | 내용 |
|--|------|
| 채택 방식 | Option B 방향 (Non-Admin 제외) + scanBasePackages 제한 |
| 수정 파일 | 신규 2개 (`AdminServerApplication.java`, `application-admin.yml`) |
| 기존 코드 영향 | 최소 (RabbitMQ Consumer에 `@ConditionalOnProperty` 추가 정도) |
| 격리 수준 | Gmail/OAuth/SSE/PubSub 등 외부 의존성 완전 제외 |
