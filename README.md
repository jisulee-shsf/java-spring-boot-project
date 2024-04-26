####  
# Giftipie
#### 📌 [프로젝트 설명]  
- 2024년 1월 26일~2024년 3월 11일(약 6주) | 백엔드 4인, 프론트 2인, 디자이너 1인 참여
- 선물 펀딩 등록과 후원이 가능한 서비스 개발 프로젝트
- 항해99 18기 7팀 중, [1위 수상](https://github.com/jisulee-shsf/save-postgresql/assets/109773795/2873f619-67c1-494f-9a20-6d2117717cbd) 🏅
##
#### 📌 [주요 역할]
- 전체 121건의 PR 중, [약 61.2%(74건)의 PR을 생성](https://github.com/Gift-For-You-Project/gift-for-you-BE/pulls?page=1&q=is%3Apr+is%3Aclosed+author%3Ajisulee-shsf)해 프로젝트에 적극적으로 참여함
- stateless, JWT, OAuth, Spring Security 등의 [핵심 개념을 사전 학습](https://jisulee-shsf.tistory.com/category/%F0%9F%93%8C%20Project%20%7C%20Giftipie/Web%20%26%20Spring)해 기능 구현의 완성도를 높임

|주요 기능|사전 학습|기술적 의사결정|트러블 슈팅|코드 구현|
|:---:|:---:|:---:|:---:|:---:|
|Spring Security를 사용한 일반 로그인 기능 구현　|[blog](https://jisulee-shsf.tistory.com/438)|[blog](https://jisulee-shsf.tistory.com/432)|-|[code](./src/main/java/com/giftforyoube/global/jwt/filter/JwtAuthenticationFilter.java)|
|OAuth 2.0에 기반한 소셜 로그인 기능 구현　|[blog](https://jisulee-shsf.tistory.com/437)|[blog](https://jisulee-shsf.tistory.com/432)|[blog](https://jisulee-shsf.tistory.com/464)|[code](./src/main/java/com/giftforyoube/user/service)|
|REST API를 통한 kakaopay 결제 기능 구현　|[blog](https://jisulee-shsf.tistory.com/434)|[blog](https://jisulee-shsf.tistory.com/433)|-|[code](./src/main/java/com/giftforyoube/donation/service/DonationService.java)|
|Access token & Refresh token 사용 로직 구현　|[blog](https://jisulee-shsf.tistory.com/431)|[blog](https://jisulee-shsf.tistory.com/415)|[blog](https://jisulee-shsf.tistory.com/457)|[code](./src/main/java/com/giftforyoube/global/jwt/filter/JwtAuthorizationFilter.java)|
##
#### 📌 [구현 결과]
- 회원가입 및 로그인 기능: 유저 테스트 기준, 총 29건의 평균 평점은 9점이며 10점이 55.2%(16건)로 가장 높은 비율을 차지함
  
|![image](https://github.com/jisulee-shsf/save-postgresql/assets/109773795/fd7579d2-278d-41dd-8ecb-f012a170627f)|![image](https://github.com/jisulee-shsf/save-postgresql/assets/109773795/d61b76ca-1655-4239-b2ac-3760a688a000)|![image](https://github.com/jisulee-shsf/save-postgresql/assets/109773795/9b35f028-3b77-477f-9130-e7f6c7bfdbb8)|![image](https://github.com/jisulee-shsf/save-postgresql/assets/109773795/aad0e02e-745a-4c8b-bba7-4e86bde78a06)|
|:---:|:---:|:---:|:---:|

- 결제 기능: 유저 테스트 기준, 총 30건의 평균 평점은 9점이며 10점이 53.3%(16건)로 가장 높은 비율을 차지함
  
|![image](https://github.com/jisulee-shsf/save-postgresql/assets/109773795/318dd62a-a6b3-47cd-9096-fe976a0251dd)|![image](https://github.com/jisulee-shsf/save-postgresql/assets/109773795/be9832c8-ea19-4f0d-ad0d-f4a778421450)|![image](https://github.com/jisulee-shsf/save-postgresql/assets/109773795/0c48a130-a16b-45cb-a12e-38fd991b58de)|
|:---:|:---:|:---:|

####
