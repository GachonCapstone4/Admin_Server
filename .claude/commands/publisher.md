Admin 서버에서 mlops를 위한 publisher 가 필요하다.
기존의 RabbitMQ 내의 폴더의 publisher/comsumer 등은 
Project를 톺아보면 알수있듯이 사용되지 않는다 따라서 별도의 새로운 Class를 
생성한다.  AdminPublisher.java 에 로직을 정의한다.
Exchange는 x.app2ai.direct 
routing key는 2ai.deployment.
해당 큐에 담길 메시지내용은 admin 의 user_id 와 deployment임을 식별할 수 있는 최소한의 필드만 담기면된다.

관리자웹에서 실행하기 때문에 API 역시 개발해야한다.
SagemakerJobController 에 POST API /modeldeploy 개발
publish 메서드를 호출한다.