# 중급 문법
## 목차
- 프로젝션과 결과 반환 - 기본
- 프로젝션과 결과 반환 - DTO 조회
- 프로젝션과 결과 반환 - @QueryProjection
- 동적 쿼리 - BooleanBuilder 사용
- 동적 쿼리 - Where 다중 파라미터 사용
- 수정, 삭제 벌크 연산
- SQL function 호출하기
___
## 프로젝션과 결과 반환 - 기본
프로젝션: select 대상 지정
- <b>프로젝선 대상이 하나</b>
    ```java
    @Test
    void simpleProjection() throws Exception {
        // given
        // when
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        // then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }    
    ```
    - 프로젝션 대상이 하나면 타입을 명확하게 지정할 수 있음
    - 프로젝션 대상이 `둘 이상`이면 `튜플`이나 `DTO`로 조회
- <b>튜플 조회</b><br>프로젝션 대상이 둘 이상일 때 사용<br>`com.querydsl.core.Tuple`
    ```java
    @Test
    void tupleProjection() throws Exception {
        // given
        // when
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        // then
        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }
    ```
___
## 프로젝션과 결과 반환 - DTO 조회
순수 JPA에서 DTO 조회
- <b>MemberDto</b>
    ```java
    @Data
    @NoArgsConstructor
    public class MemberDto {

        private String username;
        private int age;

        public MemberDto(String username, int age) {
            this.username = username;
            this.age = age;
        }
    }
    ```
- <b>순수 JPA에서 DTO 조회 코드</b>
    ```java
    @Test
    void findDtoByJPQL() throws Exception {
        // given
        // when
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        // then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }
    ```
    - 순수 JPA에서 DTO를 조회할 때는 `new 명령어`를 사용해야함
    - DTO의 package 이름을 다 적어야 함 -> 지저분함
    - `생성자 방식`만 지원함
### Querydsl 빈 생성(Bean population)
결과를 DTO 반환할 때 사용<br>
다음 3가지 방법 지원
- 프로퍼티 접근
- 필드 직접 접근
- 생성자 사용<br><br>
- <b>프로퍼티 접근 - setter</b>
    ```java
    @Test
    void findDtoBySetter() throws Exception {
        // given
        // when
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        // then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }
    ```
- <b>필드 직접 접근</b>
    ```java
    @Test
    void findDtoByField() throws Exception {
        // given
        // when
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        // then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }
    ```
    - getter or setter가 없어도 접근 가능!
- <b>별칭이 다를 때</b>
    ```java
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public class UserDto {

        private String name;
        private int age;
    }
    ```
    ```java
    @Test
    void findUserDto() throws Exception {
        // given
        QMember memberSub = new QMember("memberSub");

        // when
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        ExpressionUtils.as(
                                JPAExpressions
                                    .select(memberSub.age.max())
                                    .from(memberSub), "age")
                        )
                )
                .from(member)
                .fetch();

        // then
        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }
    ```
    - 프로퍼티나, 필드 접근 생성 방식에서 이름이 다를 때, 해결 방안
    - `username.as("memberName")`: 필드에 별칭 적용
    - `ExpressionUtils.as(source, alias)`: 필드나, 서브 쿼리에 별칭 적용
- <b>생성자 사용</b>
    ```java
    @Test
    void findDtoByConstructor() throws Exception {
        // given

        // when
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        List<UserDto> result2 = queryFactory
                .select(Projections.constructor(UserDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        // then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

        for (UserDto userDto : result2) {
            System.out.println("userDto = " + userDto);
        }
    }    
    ```
___
## 프로젝션과 결과 반환 - @QueryProjection
- <b>생성자 + @QueryProjection</b>
    ```java
    @Data
    @NoArgsConstructor
    public class MemberDto {

        private String username;
        private int age;

        @QueryProjection
        public MemberDto(String username, int age) {
            this.username = username;
            this.age = age;
        }
    }
    ```
    - `./gradlew compileQuerydsl`
    - `QMemberDto` 생성 확인
- <b>@QueryProjection 활용</b>
    ```java
    @Test
    void findDtoByQueryProjection() throws Exception {
        // given
        // when
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        // then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }
    ```
    - 이 방법은 `컴파일러로 타입을 체크`할 수 있으므로, 가장 안전한 방법이다.
    - 다만, `DTO에 QueryDSL 어노테이션을 유지`해야하는 점과, `DTO까지 Q 파일을 생성`해야하는 `단점`이 존재한다.
        > 애플리케이션 코드가 전반적으로 QueryDSL을 많이 의존하거나, 나중에 이를 교체할 일이 없다고 판단되면, 과감하게 사용하자!
- <b>distinct</b>
    ```java
    List<String> result = queryFactory
            .select(member.username).distinct()
            .from(member)
            .fetch();
    ```
    > <b>참고:</b> distinct는 JPQL의 distinct와 같다.
___
## 동적 쿼리 - BooleanBuilder 사용
<b>동적 쿼리를 해결하는 두 가지 방식</b>
- BooleanBuilder
- Where 다중 파라미터 사용

- <b>BooleanBuilder</b>
    ```java
    @Test
    void dynamicQuery_BooleanBuilder() throws Exception {
        // given
        String usernameParam = "member1";
        Integer ageParam = 10;

        // when
        List<Member> result = searchMember1(usernameParam, ageParam);

        // then
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }
    ```
___
## 동적 쿼리 - Where 다중 파라미터 사용
- <b>Where 다중 파라미터 사용</b>
    ```java
    @Test
    void dynamicQuery_WhereParam() throws Exception {
        // given
        String usernameParam = "member1";
        Integer ageParam = 10;

        // when
        List<Member> result = searchMember2(usernameParam, ageParam);

        // then
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
                //.where(allEq(usernameCond, ageCond)) // 조합 가능
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private Predicate allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }
    ```
    - `where` 조건에 `null` 값은 무시된다.
    - 메소드를 다른 쿼리에서도 재활용할 수 있다.
    - 쿼리 자체의 가독성이 높아진다.
    - usernameEq, ageEq 같은 메소드가 많아지면, 별도의 클래스를 만들어서 관리하자.
        > ex) MemberPredicates.class
    - 조합 가능
        > ex) allEq
        - `null` 체크는 주의해서 처리해야 함
        - `null` 값이 들어오면, 체이닝이 실패하는데, 다음과 같이 사용하면 `null`값을 무시할 수 있다.
            ```java
            private BooleanBuilder ageEq2(Integer ageCond){
                return ageCond != null ? new BooleanBuilder(member.age.eq(ageCond)) : new BooleanBuilder();
            }

            private BooleanBuilder usernameEq2(String usernameCond) {
                return usernameCond != null ? new BooleanBuilder(member.username.eq(usernameCond)) : new BooleanBuilder();
            }
            ```
___
## 수정, 삭제 벌크 연산
- <b>쿼리 한 번으로 대량 데이터 수정</b>
    ```java
    @Test
    void bulkUpdate() throws Exception {
        // given
        // when

        //member1 = 10 -> DB member1
        //member2 = 20 -> DB member2
        //member3 = 30 -> DB member3
        //member4 = 40 -> DB member4
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(20))
                .execute();
        //member1 = 10 -> DB 비회원
        //member2 = 20 -> DB 비회원
        //member3 = 30 -> DB member3
        //member4 = 40 -> DB member4

        //em.clear(); -> 영속성 컨텍스트를 초기화해야 DB에서 바뀐 값을 받아온다.

        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        // then
        assertThat(result.get(0).getUsername()).isEqualTo("member1"); //벌크 연산은 영속성 컨텍스트를 변경하지 않는다.
        assertThat(result.get(1).getUsername()).isEqualTo("member2"); //벌크 연산은 영속성 컨텍스트를 변경하지 않는다.
    }
    ```
- <b>기존 숫자에 1 더하기</b>
    ```java
    @Test
    void bulkAdd() throws Exception {
        // given
        // when
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();

        // then
    }
    ```
    - 곱셈: `multiply(x)`
- <b>쿼리 한 번으로 대량 데이터 삭제</b>
    ```java
    @Test
    void bulkDelete() throws Exception {
        // given
        // when
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();

        // then
    }
    ```
> <b>주의</b>
> - JPQL batch와 마찬가지로, 영속성 컨텍스트에 있는 엔티티를 무시하고 실행되기 때문에, batch 쿼리를 실행하고 나면 영속성 컨텍스트를 초기화하는 것이 안전하다.
___
## SQL function 호출하기
SQL function은 JPA와 같이 Dialect에 등록된 내용만 호출할 수 있다.

- <b>member -> M으로 변경하는 replace 함수 사용</b>
    ```java
    @Test
    void sqlFunction() throws Exception {
        // given
        // when
        List<String> result = queryFactory
                .select(Expressions.stringTemplate(
                        "function('replace', {0}, {1}, {2})",
                        member.username, "member", "M"))
                .from(member)
                .fetch();

        // then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
    ```
- <b>소문자로 변경해서 비교</b>
    ```java
    @Test
    void sqlFunction2() throws Exception {
        // given

        // when
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(
                        Expressions.stringTemplate("function('lower', {0})", member.username)))
                .fetch();

        // then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
    ```
    - lower같은 ansi 표준 함수들은 querydsl이 상당 부분 내장하고 있으므로, 다음과 같이 처리해도 결과는 같다.
        ```java
        .where(member.username.eq(member.username.lower()))
        ```
