# 실무 활용 - 순수 JPA와 Querydsl
## 목차
- 순수 JPA 리포지토리와 Querydsl
- 동적 쿼리와 성능 최적화 조회 - Builder 사용
- 동적 쿼리와 성능 최적화 조회 - Where절 파라미터 사용
- 조회 API 컨트롤러 개발
___
## 순수 JPA 리포지토리와 Querydsl
- <b>순수 JPA 리포지토리</b>
    ```java
    @Repository
    @RequiredArgsConstructor
    public class MemberJpaRepository {

        private final EntityManager em;
        private final JPAQueryFactory queryFactory;

        public void save(Member member) {
            em.persist(member);
        }

        public Optional<Member> findById(Long id) {
            Member findMember = em.find(Member.class, id);
            return Optional.ofNullable(findMember);
        }

        public List<Member> findAll() {
            return em.createQuery("select m from Member m", Member.class)
                    .getResultList();
        }

        public List<Member> findByUsername(String username) {
            return em.createQuery("select m from Member m where m.username = :username", Member.class)
                    .setParameter("username", username)
                    .getResultList();
        }
    }
    ```
- <b>순수 JPA 리포지토리 테스트</b>
    ```java
    @SpringBootTest
    @Transactional
    class MemberJpaRepositoryTest {

        @Autowired
        EntityManager em;

        @Autowired
        MemberJpaRepository memberJpaRepository;

        @Test
        void basicTest() throws Exception {
            // given
            Member member = new Member("member1", 10);
            memberJpaRepository.save(member);

            // when
            Member findMember = memberJpaRepository.findById(member.getId()).get();
            List<Member> result1 = memberJpaRepository.findAll();
            List<Member> result2 = memberJpaRepository.findByUsername("member1");

            // then
            assertThat(findMember).isEqualTo(member);
            assertThat(result1).containsExactly(member);
            assertThat(result2).containsExactly(member);
        }
    }
    ```
### Querydsl 사용
- <b>순수 JPA 리포지토리 - Querydsl 추가</b>
    ```java
    public List<Member> findAll_Querydsl() {
        return queryFactory
                .selectFrom(member)
                .fetch();
    }

    public List<Member> findByUsername_Querydsl(String username) {
        return queryFactory
                .selectFrom(member)
                .where(member.username.eq(username))
                .fetch();
    }
    ```
- <b>Querydsl 테스트 추가</b>
    ```java
    @Test
    void basicQuerydslTest() throws Exception {
        // given
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        // when
        List<Member> result1 = memberJpaRepository.findAll_Querydsl();
        List<Member> result2 = memberJpaRepository.findByUsername_Querydsl("member1");

        // then
        assertThat(result1).containsExactly(member);
        assertThat(result2).containsExactly(member);
    }
    ```
- <b>JPAQueryFactory 스프링 빈 등록</b><br>
다음과 같이 `JPAQueryFactory`를 스프링 빈으로 등록해서 주입 받아 사용해도 된다.

    ```java
    @Bean
	public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
		return new JPAQueryFactory(entityManager);
	}
    ```
> <b>참고</b>
> - 동시성 문제는 걱정하지 않아도 된다.
> - 스프링이 주입해주는 `EntityManager`는 실제 동작 시점에 진짜 `EntityManager`를 찾아주는 프록시용 가짜 `EntityManager`이다.
> - 이 가짜 `EntityManager`는 실제 사용 시점에 트랜잭션 단위로 실제 `EntityManager`(영속성 컨텍스트)를 할당해준다.
> - 더 자세한 내용은 자바 ORM 표준 JPA 책 `13.1 트랜잭션 범위의 영속성 컨텍스트`를 참고하자.
___
## 동적 쿼리와 성능 최적화 조회 - Builder 사용
- <b>MemberTeamDto - 조회 최적화용 DTO 추가</b>
    ```java
    @Data
    public class MemberTeamDto {

        private Long memberId;
        private String username;
        private int age;
        private Long teamId;
        private String teamName;

        @QueryProjection
        public MemberTeamDto(Long memberId, String username, int age, Long teamId, String teamName) {
            this.memberId = memberId;
            this.username = username;
            this.age = age;
            this.teamId = teamId;
            this.teamName = teamName;
        }
    }
    ```
    - `@QueryProjection`을 추가했다.
    - `QMemberTeamDto`를 생성하기 위해 `./gradlew compileQuerydsl`을 한 번 실행하자.
> <b>참고</b>
> - `@QueryProjection`을 사용하면, 해당 DTO가 Querydsl을 의존하게 된다.
> - 이런 의존이 싫다면, 해당 애노테이션을 제거하고, `Projection.bean(), fields(), constructor()`을 사용하면 된다.
- <b>회원 검색 조건</b>
    ```java
    @Data
    public class MemberSearchCondition {
        //회원명, 팀명, 나이(ageGoe, ageLoe)

        private String username;
        private String teamName;
        private Integer ageGoe;
        private Integer ageLoe;
    }
    ```
### 동적 쿼리 - Builder 사용
- <b>Builder를 사용한 예제</b>
    ```java
    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {

        BooleanBuilder builder = new BooleanBuilder();

        if (StringUtils.hasText(condition.getUsername())) {
            builder.and(member.username.eq(condition.getUsername()));
        }
        if (StringUtils.hasText(condition.getTeamName())) {
            builder.and(team.name.eq(condition.getTeamName()));
        }
        if (condition.getAgeGoe() != null) {
            builder.and(member.age.goe(condition.getAgeGoe()));
        }
        if (condition.getAgeLoe() != null) {
            builder.and(member.age.loe(condition.getAgeLoe()));
        }

        return queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(builder)
                .fetch();
    }
    ```
- <b>조회 예제 테스트</b>
    ```java
    @Test
    void searchTest() throws Exception {
        // given
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        // when
        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(35);
        condition.setAgeLoe(40);
        condition.setTeamName("teamB");

        List<MemberTeamDto> result1 = memberJpaRepository.searchByBuilder(condition);

        // then
        assertThat(result1)
                .extracting("username")
                .containsExactly("member4");
    }
    ```
___
## 동적 쿼리와 성능 최적화 조회 - Where절 파라미터 사용
- <b>Where절에 파라미터를 사용한 예제</b>
    ```java
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))
                .fetch();
    }

    private BooleanExpression usernameEq(String username) {
        return StringUtils.hasText(username) ? member.username.eq(username) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return StringUtils.hasText(teamName) ? team.name.eq(teamName) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }
    ```
> <b>참고</b>
> - where 절에 파라미터 방식을 사용하면 조건 재사용 가능
> ```java
> //where 파라미터 방식은 이런식으로 재사용이 가능하다.
> public List<Member> findMember(MemberSearchCondition condition) {
>     return queryFactory
>     .selectFrom(member)
>     .leftJoin(member.team, team)
>     .where(usernameEq(condition.getUsername()),
>            teamNameEq(condition.getTeamName()),
>            ageGoe(condition.getAgeGoe()),
>            ageLoe(condition.getAgeLoe()))
>     .fetch();
> }
> ```
___
## 조회 API 컨트롤러 개발
편리한 데이터 확인을 위해 샘플 ㅔㄷ이터를 추가하자.<br>
샘플 데이터 추가가 테스트 케이스 실행에 영향을 주지 않도록 다음과 같이 프로파일을 설정하자.
- <b>프로파일 설정</b><br>`src/main/resources/application.yml`
    ```yaml
    spring:
        profiles:
            active: local
    ```
- <b>테스트는 기존 application.yml을 복사해서 다음 경로로 붙여넣고, 프로파일을 test로 수정하자.</b><br>`src/test/resources/application.yml`
    ```yaml
    spring:
        profiles:
            active: test
    ```
    - 이렇게 분리하면 main 소스코드와 테스트 소스코드 실행 시 프로파일을 분리할 수 있다.
- <b>샘플 데이터 추가</b>
    ```java
    @Profile("local")
    @Component
    @RequiredArgsConstructor
    public class InitMember {

        private final InitMemberService initMemberService;

        @PostConstruct
        public void init() {
            initMemberService.init();
        }

        @Component
        @RequiredArgsConstructor
        static class InitMemberService {
            private final EntityManager em;

            @Transactional
            public void init() {
                Team teamA = new Team("teamA");
                Team teamB = new Team("teamB");
                em.persist(teamA);
                em.persist(teamB);

                for (int i = 0; i < 100; i++) {
                    Team selectedTeam = i % 2 == 0 ? teamA : teamB;
                    em.persist(new Member("member" + i, i, selectedTeam));
                }
            }
        }
    }
    ```
- <b>조회 컨트롤러</b>
    ```java
    @RestController
    @RequiredArgsConstructor
    public class MemberController {

        private final MemberJpaRepository memberJpaRepository;

        @GetMapping("/v1/members")
        public List<MemberTeamDto> searchMemberV1(MemberSearchCondition condition) {
            return memberJpaRepository.search(condition);
        }
    }
    ```