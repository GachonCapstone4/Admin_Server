TABLE `training_jobs` (
`job_id` VARCHAR(64) NOT NULL COLLATE 'utf8mb4_uca1400_ai_ci',
`job_type` VARCHAR(50) NULL DEFAULT NULL COLLATE 'utf8mb4_uca1400_ai_ci',
`task_type` VARCHAR(50) NULL DEFAULT NULL COLLATE 'utf8mb4_uca1400_ai_ci',
`dataset_version` VARCHAR(50) NULL DEFAULT NULL COLLATE 'utf8mb4_uca1400_ai_ci',
`requested_by` VARCHAR(100) NULL DEFAULT NULL COLLATE 'utf8mb4_uca1400_ai_ci',
`status` ENUM('QUEUED','RUNNING','COMPLETED','FAILED') NULL DEFAULT NULL COLLATE 'utf8mb4_uca1400_ai_ci',
`model_version` VARCHAR(100) NULL DEFAULT NULL COLLATE 'utf8mb4_uca1400_ai_ci',
`metrics_json` LONGTEXT NULL DEFAULT NULL COLLATE 'utf8mb4_uca1400_ai_ci',
`error_message` TEXT NULL DEFAULT NULL COLLATE 'utf8mb4_uca1400_ai_ci',
`created_at` DATETIME(6) NULL DEFAULT NULL,
`started_at` DATETIME(6) NULL DEFAULT NULL,
`finished_at` DATETIME(6) NULL DEFAULT NULL,
PRIMARY KEY (`job_id`) USING BTREE,
INDEX `idx_training_status` (`status`) USING BTREE,
INDEX `idx_training_created` (`created_at`) USING BTREE
)
COLLATE='utf8mb4_uca1400_ai_ci'
ENGINE=InnoDB
;

위는 consumer가 consume 을 수행한뒤 쓰기 트랜잭션을 수행할 테이블이다.
consume 하는 대상이 되는 큐는 q.2app.training이다.
AdminConsumer가 Service 레이어 역할도 수행하는 것 검토(Service 계층 추가의 오버인제니어링 검토)
위의 내용은 전부 큐에 실려서올 것이고 위에 해당하는 값을 직렬화 하는 DTO를
기존의 TrainingJobResultMessage DTO를 수정하여 직렬화한다.
만약 consume 되는 메시지가 동일한 job_id일경우에는 변경사항이 있는부분만 트랜잭션에 쓰기를 진행한다.
소비 실패 시  nack 를 반환해야한다.
현재 레포지토리가 Trainig(model/dataset/job) 이렇게 지나치게 많이 나뉘어져있는데 
제거하고 MlopsRepository 를 하나 생성하여 여기서 트랜잭션 쿼리 수행.
Trainingjob엔티티는 유지해도 될것 같다.
또한 기존의 service 서버와 admin 서버의 분리로 기존 MailConsumer는 사용되지 않는다. 본 서버는 admin서버이다.
안전하게 MailConsumer와 연관 소스 삭제하고

위 설계 검토해주고 기존코드 충돌부분이 있는지 체크한번 진행 문제 없을 시 코딩 여부 물어볼것.
