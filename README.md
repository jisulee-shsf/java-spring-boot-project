<img width="879" alt="항해18기 최고 인기 프로젝트상 Giftipie" src="https://github.com/95hyun/Algorithm/assets/151743721/7da5f361-1875-4a98-915b-f28b914b85aa">

<img width="879" alt="스크린샷 2024-03-05 오전 2 41 59" src="https://github.com/Gift-For-You-Project/gift-for-you-BE/assets/151743721/54402b89-3772-4603-bcef-89f847dcedea">

# 기프티파이(Giftipie)
기프티파이에서 정말 원하는 선물을 주고 받아요!

**원하는 선물을 받고 싶을 때**, 한 사람을 통해 받기에는 부담되는 가격의 선물을 받고 싶을 때가 있습니다.
이때 **선물 펀딩을 등록하여 지인들에게 링크를 공유하고, 지인들이 원하는 금액만큼 후원할 수 있는 서비스**입니다.


🎁 [Giftipie 바로가기](https://www.giftipie.me/)

🖥️ [Front-End Github](https://github.com/Gift-For-You-Project/gift-for-you-FE)

💻 [Back-End Github](https://github.com/Gift-For-You-Project/gift-for-you-BE)

📓 [기프티파이 노션](https://www.notion.so/a2046b341d5b416f8a815eb41377f9c4?pvs=21)

📘 [기프티파이 브로셔](https://hiryuji.notion.site/Giftipie-a7b9052006cf4e269b900096e1d0d616?pvs=74)


## 팀원소개

![image](https://github.com/Gift-For-You-Project/gift-for-you-BE/assets/151743721/4804d97f-f9c0-49e4-8bbb-c23bc59b9d2c)

| 이름 | 분담 | 깃허브 |
| --- | --- | --- |
| 고훈(VL) | 펀딩 CRUD, SWAGGER, S3 이미지 업로드 기능, 펀딩 리스트 (전체, 진행중, 완료된 펀딩) 페이지네이션 기능 및 무한스크롤 기능 구현, 구글 애널리틱스 연동 | https://github.com/LyricZen | 
| 현민영 | CI-CD, AWS 서버 관리, Nginx, SSL, 모니터링 구현, 펀딩 수정/삭제, 내 펀딩 조회, 실시간 알림기능(SSE), 알림 이메일 발송, 회원가입 시 이메일 검증 메일 기능, 테스트코드 작성(NotificationService), UT 총무 | https://github.com/95hyun |
| 김도현 | 펀딩 CRUD, Redis 캐시 적용 및 캐시 무효화, 메타 태그 크롤링을 통한 링크 상품 이미지 등록, Scheduler를 이용한 펀딩 자동 갱신, 펀딩 통계(Summary) 계산, D-Day 및 목표 금액 달성율 계산, Redisson 락 적용(링크 상품 등록, 펀딩 생성/수정/삭제) | https://github.com/DoKkangs |
| 이지수 | 회원가입, 로그인(Spring Security), 로그아웃, 회원탈퇴 기능 구현 / Spring Security 기반 인증 및 인가 기능 구현 / 소셜 로그인(Kakao & Google) 기능 구현 / Access & Refresh token 사용 로직 설계 및 기능 구현 / Kakaopay 온라인 결제 API & Kakaopay 데모 연동을 통한 결제 기능(준비 & 승인) 구현 / Global Exception Handler를 사용한 전역 예외 처리 기능 구현 / Github Organization 생성 및 관리 | https://github.com/jisulee-shsf |


## 📆 프로젝트 기간

- 2024.01.26 ~ 2024.03.11

###
## 🏗️ 서비스 아키텍처

### ✅ 전체 아키텍처
<img width="1231" alt="Giftipie 전체 아키텍처" src="https://github.com/95hyun/Algorithm/assets/151743721/0628f865-7998-4587-86e7-d8645c590e68">

### ✅ Back-End
<img width="1060" alt="giftipie아키텍처백엔드" src="https://github.com/95hyun/Algorithm/assets/151743721/caf183e2-a5db-422c-be70-edd8271a1b6f">


## 📌 기술적 의사결정

| 📌 사용 기술 | 📖 기술 설명 |
| --- | --- |
| GitHub Actions | GitHub와의 통합이 용이하며 비교적 설정이 간단하고, 빠른 배포와 프로젝트의 규모가 작은 경우 유리하기 때문에 해당 기술을 선택하였습니다. |
| Docker | 독립적인 환경을 구성하고, 개발 환경과 운영 환경 간의 일관성을 유지하며 컨테이너 기반의 배포로 가볍게 배포할 수 있기 때문에 해당 기술을 선택하였습니다. |
| Blue-Green | 사용자에게 영향을 주지 않으면서 신규 버전을 안전하게 테스트하고 점진적으로 전환할 수 있으며, blue-green 두 환경이 독립적이기 때문에 새 버전의 오류가 기존 시스템에 영향을 미치지 않는 이점으로 해당 기술을 선택하였습니다. |
| Nginx | 한정된 예산을 사용하는 상황에서 하나의 EC2 인스턴스로 서버를 구축하였기 때문에, nginx의 리버스 프록시 기능을 통해 한대의 서버로 무중단배포를 구현하였습니다. |
| SSE (Server-Sent Events) | 서버에서 클라이언트로의 메세지 전달만 필요했기 때문에 단방향 통신 기술인 SSE가 가장 적합한 기술이라 판단하여 선택하였습니다. |
| Social Login(Kakao, Google) | 펀딩 후원에 참여하기 위해 다수가 접근할 수 있는 점을 고려하여, 사용자의 접근성에 중점을 둔 소셜 로그인(Kakao & Google) 기능 구현을 선택하였습니다.  |
| Spring Security | 인증되지 않은 불특정 다수가 접근할 수 있는 점을 고려하여, 개인정보 보안성에 중점을 둔 Spring Security 기반의 로그인 기능 구현을 선택하였습니다. |
| Kakaopay Online Payment API | 원하는 펀딩에 후원을 진행하고, 후원 결제 내역을 수집하기 위해 Kakaopay 온라인 결제 기능 구현을 선택하였습니다. |
| Redis | 사용자들에게 빈번하게 보여지는 정보들은 캐시를 적용하여 처리하면 성능 개선을 할 수 있을 것이라고 생각하였고, 추후 사용자가 늘어남에 따라 동시성 문제도 발생할 수 있다고 생각하여 이를 제어할 수 있는 기능을 제공하는 해당 기술을 선택하였습니다. |
| Meta tag 크롤링 | 사용자가 이미지를 직접 등록하는 것보다는 링크를 입력해서 자동으로 해당 페이지의 링크나 사진을 가져오는게 편할 것이라고 생각하여 기술을 선택하였습니다. |
| Prometheus, Grafana, Node Exporter, Slack | 한정된 예산 상황에 맞춰 낮은 성능의 서버로 개발을 진행한 상태라, 유저테스트 시 서버 상태를 모니터링하여 스케일업을 대비하기 위해 기술을 선택하였습니다.  |
| Google Analytics | 유저테스트 시에 유저들이 어느 페이지에서 오래 머물렀는지, 어느 페이지에서 몇번 클릭했는지, 접속 환경, 조회수 등에 대한 정보를 얻을 수 있어 유저 경험 보완에 도움이 될 것으로 판단되어 기술을 선택하였습니다. |



## 🔧 트러블 슈팅
    
## ✔️ blue-green 무중단 배포 시 50% 확률로 502 Bad Gate Way
    
`문제상황`

CI-CD를 통해 자동으로 배포가 이루어질 때, 포트 전환이 되면서 50%확률로 502 Bad Gateway 페이지가 나왔다.

`원인파악`

로드밸런스 대상그룹의 헬스체크의 딜레이 문제로 서버 안정화가 되지 않았을때 새로운 컨테이너로 호스팅을 연결해서 나타난 문제

`해결방법`

배포 스크립트에 AWS CLI에 접근하는 코드를 추가하고 대상그룹의 상태검사 체크 여부를 확인하는 코드를 추가하고, health check 딜레이를 감안하여 30의 sleep을 주고, 대상그룹의 상태검사편집을 통해 딜레이를 최소화하여 502 문제를 해결하였다.

## ✔️ SSE Emitter 연결이 1분마다 끊기는 현상

`문제상황`

로그인에 성공하여 자동으로 서버와 클라이언트가 SSE 연결이 되는데, 백엔드 코드 상에서는 Time Out 시간을 1시간으로 설정했지만, 1분이 지나면 자동으로 계속 연결이 끊기는 상황이 발생

`원인파악`

개발자 도구 콘솔에 **ERR_HTTP2_PROTOCOL_ERROR 200** 에러 코드가 찍혀있는 것으로 보아 코드상의 문제가 아니라 서버 환경 설정에 문제가 있는 것으로 추론. Nginx 사용시 HTTP 프로토콜 버전이 자동으로 1.0으로 설정되어 SSE 연결이 유지 불가능함을 확인.

`해결방법`

1. Nginx와 로드밸런서를 같이 사용 중이면 로드밸런서에서 자동으로 HTTP/2 를 기본으로 설정해주기 때문에 별도의 Nginx 설정이 필요없었음.
2. AWS ALB의 ‘트래픽 구성 - 유휴 제한 시간’을 4000초 (1시간 이상)으로 설정하여 코드 상의 1시간 Time Out 설정이 동작할 수 있도록함.
    
## ✔️ 만료된 Access token 내 Claim 추출 불가 건
    
[[Project | 트러블 슈팅] 만료된 Access token 내 Claim 추출 불가 건](https://jisulee-shsf.tistory.com/457)

`문제상황`

만료된 access token에서 claim을 추출할 경우, ServletException이 지속적으로 발생함

이에 클라이언트가 만료된 access token으로 API를 요청할 경우, 유저를 식별하는 email을 추출하지 못해 아래 로직을 수행하지 못함

- claim 내 email로 DB 내 refresh token 유효 여부를 판별할 수 없음
- claim 내 email로 새로운 access token 재발급이 불가함

`원인파악` 

보안상의 이유로 인해, 만료된 access token에서 claim을 추출할 수 없음을 파악함

`해결방법`

로그인 시 발급된 access token을 DB에 저장함

이에 클라이언트가 만료된 access token으로 API를 요청할 경우, 만료된 access token으로 유저를 식별해 아래 로직을 수행함

- 식별된 유저의 refresh token으로 DB 내 refresh token 유효 여부를 판별함
- 식별된 유저의 email로 새로운 access token을 재발급함
    
## ✔️ Redis 날짜/시간 데이터 직렬화, 역직렬화 문제

`문제상황`

날짜 데이터를 직렬화/역직렬화 하는 과정에서 LocalDateType을 지원하지 않는다는 오류 발생

```bash
2024-02-06T11:42:21.857+09:00 ERROR 13956 --- [nio-8080-exec-3] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed: org.springframework.data.redis.serializer.SerializationException: Could not write JSON: Java 8 date/time type `java.time.LocalDate` not supported by default: add Module "com.fasterxml.jackson.datatype:jackson-datatype-jsr310" to enable handling (through reference chain: com.giftforyoube.funding.dto.FundingResponseDto["endDate"])] with root cause
```

`원인파악`

**`java.time.LocalDate`**와 같은 Java 8 날짜/시간 타입을 기본적으로 지원하지 않는 **`ObjectMapper`** 설정 때문에 발생하였습니다.

`해결방법`

처음에는`Jackson2JsonRedisSerializer`를 사용해서 날짜 데이터를 직렬화/역직렬화 할 수있게 `ObjectMapper`에 JavaTimeModule을 추가해 설정을 했습니다.

```java
@Bean
public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
  RedisTemplate<String, Object> template = new RedisTemplate<>();
  template.setConnectionFactory(redisConnectionFactory);

  Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
  ObjectMapper objectMapper = new ObjectMapper();
  objectMapper.registerModule(new JavaTimeModule());
  objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
  objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.WRAPPER_ARRAY);
  serializer.setObjectMapper(objectMapper);  //스프링부트 3.0 이상부터 deprecated  

  template.setKeySerializer(new StringRedisSerializer());
  template.setValueSerializer(serializer);

  return template;
}
```

하지만 `ObjectMapper`의 `setObjectMapper`가 스프링부트 3.0 이상부터 deprecated 되었다고 하여 커스텀한 `serializer`를 만들어서 해결하기로 했습니다.

```java
@Bean
public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
   RedisTemplate<String, Object> template = new RedisTemplate<>();
   template.setConnectionFactory(redisConnectionFactory);
   // Key Serializer
   template.setKeySerializer(new StringRedisSerializer());
   // Value Serializer
   template.setValueSerializer(customRedisSerializer());
   return template;
}

@Bean
public RedisSerializer<Object> customRedisSerializer() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule()); // JavaTimeModule 등록
    objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

    return new RedisSerializer<Object>() {
        @Override
        public byte[] serialize(Object t) throws SerializationException {
            try {
                return objectMapper.writeValueAsBytes(t);
            } catch (Exception e) {
                throw new SerializationException("Error serializing object to JSON", e);
            }
        }

        @Override
        public Object deserialize(byte[] bytes) throws SerializationException {
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            try {
                return objectMapper.readValue(bytes, Object.class);
            } catch (Exception e) {
                throw new SerializationException("Error deserializing object from JSON", e);
            }
        }
    };
}
```

이렇게 커스텀한 `serializer`설정을 사용하여 Redis에 저장되는 객체들이 `JavaTimeModule`을 통해 `java.time` 패키지 타입을 올바르게 처리할 수 있어 날짜/시간 타입의 직렬화/역직렬화 문제를 해결할 수 있었습니다.



## 🔎 주요기능

## ✅ 회원가입 / 로그인

### 📌 일반 회원가입 / 로그인(Spring Security)

|일반 회원가입|일반 로그인|
|:--:|:--:|
|![image](https://github.com/Gift-For-You-Project/gift-for-you-BE/assets/151743721/cbb377ee-2f31-4e31-b42b-bd6f430c8e3a)|![image](https://github.com/Gift-For-You-Project/gift-for-you-BE/assets/151743721/10deb040-250b-4881-b3d4-4bcca6f6eb0a)|

- 유효성 검증과 약관 동의가 포함된 회원가입을 할 수 있습니다.
- Spring Security로 사용자의 개인정보 보안성에 중점을 둔 로그인을 할 수 있습니다.

### 📌 일반 회원가입 시 이메일 인증

![회원가입인증메일짤](https://github.com/Gift-For-You-Project/gift-for-you-BE/assets/151743721/18a7f154-af62-4094-ae08-86cb99447975)

- 실제 사용 중인 이메일인지 인증 메일을 발송하고, 인증 코드를 발급하여 메일을 인증할 수 있습니다.
    
### 📌 소셜 로그인(Kakao, Google)
    
![image](https://github.com/Gift-For-You-Project/gift-for-you-BE/assets/151743721/04c830ba-a8ed-4ae9-8376-b8ec1eb68e29)
    
- 사용자의 접근성에 중점을 둔 소셜 로그인(Kakao & Google)을 할 수 있습니다.
    
## ✅ 펀딩 등록
    
![펀딩등록짤](https://github.com/Gift-For-You-Project/gift-for-you-BE/assets/151743721/90b78d0b-2855-4650-ba14-0a9840e209d9)
    
- 상품 정보가 있는 **링크**를 입력하여 이미지를 등록할 수 있습니다. 
- 이미지 등록 후 나머지 정보들을 모두 입력하여 펀딩을 등록할 수 있습니다.
    
## ✅ 펀딩 조회
    
![펀딩조회짤](https://github.com/Gift-For-You-Project/gift-for-you-BE/assets/151743721/c99b0b21-7064-46da-9248-83af3fe8b4ef)
    
- **최근 펀딩 구경하기 - 펀딩더보기** 를 눌러 최근 순으로 다른 유저들의 펀딩을 볼 수 있습니다.
- 전체, 진행중, 완료 탭을 눌러 상태별로 확인이 가능합니다.
    
## ✅ Kakaopay를 통한 후원(결제) 기능
    
![후원결제짤](https://github.com/Gift-For-You-Project/gift-for-you-BE/assets/151743721/0f528bb3-6350-4a9a-a496-3bba33938c20)
|1|2|3|4|
|:--:|:--:|:--:|:--:|
|![image](https://github.com/Gift-For-You-Project/gift-for-you-BE/assets/151743721/b6ad4b27-693a-4ae5-9205-2c0522c589e9)|![image](https://github.com/Gift-For-You-Project/gift-for-you-BE/assets/151743721/1154407b-2e49-45f1-b205-05cc7eccc989)|![image](https://github.com/Gift-For-You-Project/gift-for-you-BE/assets/151743721/76857c5c-74c8-4e2e-8918-fda270a4a950)|![image](https://github.com/Gift-For-You-Project/gift-for-you-BE/assets/151743721/bec6d585-10ae-4478-9dcd-000ea168ff80)|
    
- Kakaopay 온라인 결제를 통해 원하는 펀딩에 테스트 결제로 후원할 수 있습니다.
    
## ✅ Giftipie에서 함께한 선물 - 통계 기능
    
![image](https://github.com/Gift-For-You-Project/gift-for-you-BE/assets/151743721/ed6e5c26-d232-4162-88a0-06b92553ed35)

- 펀딩에 참여한 총 인원, 목표 금액 달성으로 펀딩이 종료되어
  선물을 받은 인원, Giftipie에서 이루어진 펀딩의 총 금액을 계산하여 페이지에 보여줍니다. 
    
## ✅ 실시간 알림 기능

![후원알림짤](https://github.com/Gift-For-You-Project/gift-for-you-BE/assets/151743721/09a9ca0d-f173-4ada-b577-5bc078386994)

- 페이지 상단에 실시간 알림을 띄워줍니다. 
  실시간 알림은 **후원발생, 펀딩성공, 펀딩마감** 시에 동작합니다.

![알림목록짤](https://github.com/Gift-For-You-Project/gift-for-you-BE/assets/151743721/ce83adf0-f128-45fc-8b8d-470f1677aa4b)

- 우측 상단의 종모양 버튼을 누르면 알림 목록 페이지로 이동되어 받았던 알림메시지들을 확인할 수 있습니다.
- 해당 알림메시지를 누르면 관련 페이지로 이동합니다.
- 원하는 메시지를 선택하여 삭제할 수 있고, 읽은 메시지를 한번에 삭제할 수 있습니다.

![image](https://github.com/Gift-For-You-Project/gift-for-you-BE/assets/151743721/59fb96c3-f996-49f0-86ac-454a8faea09c)

- 회원 가입시에 이메일 수신 동의에 체크했다면 실시간 알림 발생 시 사이트에 접속 중이 아니더라도, 
  이메일로 알림 이메일을 받아볼 수 있습니다.
