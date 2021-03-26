# 라이브러리 살펴보기
- Gradle은 의존관계가 있는 라이브러리를 함께 다운로드 한다.

## 스프링 부트 라이브러리
### spring-boot-starter-web
- `spring-boot-starter-tomcat` : 톰캣(웹서버)
- `spring-webmvc` : 스프링 웹 MVC
    - `Spring web MVC란?`
        - Spring Framework에서 제공하는 `web module`이다.
        - MVC는 `Model-View-Controller`의 약자로, 기본 시스템 모듈을 MVC로나누어   구현한다.
            - `Model` : 데이터 디자인을 담당
                > ex) 상품 목록, 주문 내역 등
            - `View` : 실제로 렌더링되어 보이는 페이지를 담당
                - `렌더링(rendering)` : server로 부터 HTML 파일을 받아 webbrowser에    전달하는 과정
                > ex) `.JSP` 파일들이 여기에 해당
            - `Controller` : 사용자의 request를 받고 response를 주는 logic을담당
            > ex) `Get`, `Post` 등의 uri mapping이 여기에 해당
### spring-boot-starter-thymeleaf(View)
- Web 및 독립형 환경을 위한 modern sever-side Java templateengine이다.
- HTML은 web browser 출력하는 것 뿐만 아니라, static prototype의역할도 가능하다.
- 주요 목표
    - Development workflow에 우아하고 `자연스러운 template`을 가져오는것
         ```html
        <table>
            <thead>
              <tr>
                <th th:text="#{msgs.headers.name}">Name</th>
                <th th:text="#{msgs.headers.price}">Price</th>
              </tr>
            </thead>
            <tbody>
              <tr th:each="prod: ${allProducts}">
                <td th:text="${prod.name}">Oranges</td>
                <td th:text="${#numbers.formatDecimal(prod.price, 1,2)}">0.99</td>
              </tr>
            </tbody>
        </table>
        ```           
- text, HTML, XML, JS, CSS 등을 생성할 수 있는 template engine이다.
- 순수 HTML으로 template을 작성할 수 있다.
- Spring Boot에서 사용이 권장된다. (JSP는 추천하지 않음)
### spring-boot-starter(공통)
- 스프링 부트 + 스프링 코어 + 로깅
- spring-boot
    - spring-core
- spring-boot-starter-logging
    - logback, slf4j
    - logging tool을 사용하는 이유
        - System.out.println() 명령어는 I/O resources를 많이 사용하여 system이 느려질 수 있기에, log를 file로 저장하여 분석할 필요가 있기 때문이다.
        
## 테스트 라이브러리
- `spring-boot-starter-test`
    - `junit` : 테스트 프레임워크
        - Java에서 독립된 단위 테스트를 지원해주는 framework
    - `mockito` : 목 라이브러리
        - `Mock` : 객체와 비슷하게 동작하지만, 프로그래머가 직접 그 객체의 행동을 관리하는 객체
        - `Mockito` : Mock 객체를 쉽게 만들고, 관리, 검증할 수 있는 방법을 제공
        - [Reference](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
    - `assertj` : 테스트 코드를 좀 더 편하게 작성하게 도와주는 라이브러리
        - 테스트 대상 지정하기
            - `assertThat(테스트 타켓).메소드1().메소드2().메소드3();`
        - 문자열 테스트
            ```java
            assertThat("Hello, world! Nice to meet you.") // 주어진 "Hello, world! Nice to meet you."라는 문자열은
				.isNotEmpty() // 비어있지 않고
				.contains("Nice") // "Nice"를 포함하고
				.contains("world") // "world"도 포함하고
				.doesNotContain("ZZZ") // "ZZZ"는 포함하지 않으며
				.startsWith("Hell") // "Hell"로 시작하고
				.endsWith("u.") // "u."로 끝나며
				.isEqualTo("Hello, world! Nice to meet you."); // "Hello, world! Nice to meet you."과 일치합니다.
            ```
        - 숫자 테스트
            ```java
            assertThat(3.14d) // 주어진 3.14라는 숫자는
				.isPositive() // 양수이고
				.isGreaterThan(3) // 3보다 크며
				.isLessThan(4) // 4보다 작습니다
				.isEqualTo(3, offset(1d)) // 오프셋 1 기준으로 3과 같고
				.isEqualTo(3.1, offset(0.1d)) // 오프셋 0.1 기준으로 3.1과 같으며
				.isEqualTo(3.14); // 오프셋 없이는 3.14와 같습니다
            ```
    - `spring-test` : 스프링 통합 테스트 지원
        - `@RunWith`, `@ContextConfiguration` 등의 어노테이션을 활용
            - `Annotation(어노테이션)` : 본래 주석이란 뜻으로, 인터페이스를 기반으로 한 문법이다. 주석처럼 코드에 달아 클래스에 `특별한 의미`를 부여하거나 `기능을 주입`할 수 있다.
