# 🎯 [Project Task] Spring Boot Admin 서버 - Kubernetes Job 실행 기능 구현 가이드

## 1. 개요 및 목적
Admin 서버에서 Kubernetes Cluster 내에 특정 Job을 실행하는 기능을 구현합니다.
본 기능의 핵심 설계 원칙은 **"Job의 형태와 스펙(YAML)은 절대 Spring 내부에서 동적으로 조립되거나 생성되지 않아야 한다"**는 것입니다. Spring은 단지 Git(GitHub)에 저장된 정적 YAML 파일을 읽어와 Kubernetes API에 전달하는 '실행자(Executor)' 역할만 수행해야 합니다.

## 2. 절대 준수 규칙 (Strict Constraints)
- 🚫 **No Dynamic YAML Generation:** Spring 코드 내부에서 `JobBuilder` 등을 사용하여 Job 스펙을 정의하거나, YAML 파일의 핵심 구조(Containers, Image, Command 등)를 변형해서는 안 됩니다.
- ✅ **Git as a Single Source of Truth:** 실행될 Job의 YAML 원천은 반드시 GitHub Repository의 특정 경로에 있는 파일이어야 합니다.
- ✅ **In-Cluster Configuration:** Admin 서버는 Kubernetes Pod 내에서 동작하며, 할당된 ServiceAccount의 RBAC 권한을 통해 Kube API 서버와 통신합니다.

## 3. 필요 의존성 (Dependencies)
구현을 위해 다음 기능에 해당하는 라이브러리를 고려하여 `build.gradle` (또는 `pom.xml`) 설정을 제안해 주세요.
1. **Kubernetes API 통신:** YAML 파일을 로드하고 파싱하여 Kube API로 전송하기 쉬운 클라이언트 (예: `io.fabric8:kubernetes-client` 권장. YAML stream 로드 기능이 우수함)
2. **GitHub API 연동:** 외부 GitHub Repository에서 YAML 파일의 Raw 데이터를 읽어오기 위한 HTTP 클라이언트 (예: `WebClient`, `RestTemplate` 또는 GitHub 공식 API 클라이언트)

## 4. 비즈니스 로직 흐름 (Workflow)
구현해야 할 서비스 클래스의 흐름은 다음과 같습니다.

1. **Trigger:** 클라이언트 또는 Admin 스케줄러로부터 특정 Job 실행 요청(Job 식별자 등 포함)을 받습니다.
2. **Fetch YAML from GitHub:** - 요청받은 Job에 매핑되는 대상 GitHub Repository의 YAML 파일 Raw Data(URL)를 호출하여 문자열 또는 InputStream 형태로 가져옵니다. (필요시 Private Repo를 위한 PAT 토큰 헤더 처리 포함)
3. **Load & Parse:** - 가져온 YAML 데이터를 K8s Client를 이용해 Kubernetes `Job` 객체로 파싱합니다.
    - *주의: 이 단계에서 YAML의 뼈대를 수정하지 않습니다.*
4. **Deploy to Kubernetes:**
    - 파싱된 Job 객체를 현재 Admin 서버가 속한 Namespace (또는 지정된 Namespace)에 배포(Create)합니다.
5. **Response:**
    - 생성된 Job의 이름, Namespace, UID 등의 메타데이터를 반환하여 Admin 서버가 추적할 수 있도록 합니다.

## 5. 코딩 에이전트에게 요구하는 산출물 (Expected Output)
위 가이드라인을 바탕으로 다음 항목들을 작성해 주세요.

1. **Gradle/Maven 의존성 설정 코드**
2. **GitHub Repository 연동 설정 (application.yml 예시)**
3. **핵심 로직이 담긴 Service 클래스 코드 (Java/Kotlin)**
    - GitHub API를 통해 Raw YAML을 읽어오는 메서드
    - `Fabric8` (또는 Official Java Client)를 사용해 YAML을 Load하고 Cluster에 Create하는 메서드
    - 예외 처리 (YAML 파싱 실패, GitHub 통신 실패, K8s RBAC 권한 부족 등)
   


