## 추가사항
git&job.md는 이미 구현되었다.
현재구조에서 Git 레포의 GITHUB_YAML_BASE_PATH 안에는 여러개의 job manifest가 존재한다.
내가 원하는 기능은 하나의 api 경로당 하나의 job이 매핑되어 클라이언트에서 각각의 버튼을 통해서 각각의 job을 수행시키는 것을 원한다.
하나의 job만 수행하는 구조로 진행할 것이다. 따라서 Spring SOLID 하게 구현하려면? 어떤구조로 가는것이 권장되나?
Job 식별자 Enum을 활용한 컨트롤러 라우팅 방법을대표적으로 고려