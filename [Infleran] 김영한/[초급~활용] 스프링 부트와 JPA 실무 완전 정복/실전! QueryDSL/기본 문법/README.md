# 기본 문법
## 목차
- JPQL vs Querydsl
- 기본 Q-Type 활용
- 검색 조건 쿼리
- 결과 조회
- 정렬
- 페이징
- 집합
- 조인 - 기본 조인
- 조인 - on절
- 조인 - Fetch join
- 서브 쿼리
- Case 문
- 상수, 문자 더하기

<b>테스트 기본 코드</b>

```java
SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);

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
    }
}
```
___
## JPQL vs Querydsl
- <b>Queryds vs JPQL</b>
    ```java
    @Test
    void startJPQL() throws Exception {
        // given

        // when
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        // then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void startQuerydsl() throws Exception {
        // given
        QMember m = new QMember("m");

        // when
        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1")) //파라미터 바인딩 처리
                .fetchOne();

        // then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }
    ```
    - `EntityManager`으로 `JPAQueryFactory` 생성
    - Querydsl은 `JPQL 빌더 역할`이다.
    - JPQL: 문자(실행 시점 오류), Querydsl: `코드(컴파일 시점 오류)`
    - JPQL: 파라미터 바인딩 직접, Querydsl: `파라미터 바인딩 자동 처리`
- `JPAQueryFactory`를 필드로 제공하면, 동시성 문제는 어떻게 될까?
    - `JPAQueryFactory`를 생성할 때, 제공하는 `EntityManager`에 달려 있다.
    - 스프링 프레임워크는 여러 Threads에서 동시에 같은 `EntityManager`에 접근해도, `트랜잭션마다 별도의 영속성 컨텍스트`를 제공하기 때문에, 동시성 문제는 걱정하지 않아도 된다.
___
## 기본 Q-Type 활용
- <b>Q클래스 인스턴스를 사용하는 2가지 방법</b>
    ```java
    QMember qmember = new QMember("m"); //별칭 직접 지정
    QMember qmember = QMember.member; //기본 인스턴스 사용
    ```
- <b>기본 인스턴스를 static import와 함께 사용</b>
    ```java
    @Test
    void startQuerydsl() throws Exception {
        // given

        // when
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1")) //파라미터 바인딩 처리
                .fetchOne();

        // then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }
    ```
- 다음 설정을 추가하면 실행되는 JPQL을 볼 수 있다.
    ```properties
    spring.jpa.properties.hibernate.use_sql_comments: true
    ```
> <b>참고</b>
> - 같은 테이블을 조인해야 하는 경우가 아니면, 기본 인스턴스를 사용하자.
___
## 검색 조건 쿼리
- <b>기본 검색 쿼리</b>
    ```java
    @Test
    void search() throws Exception {
        // given
        // when
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        // then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }
    ```
    - 검색 조건은 `.and()`, `.or()` 메소드 체인으로 연결할 수 있다.
> <b>참고</b>
> - `select`, `from`을 `selectFrom`으로 합칠 수 있음
- <b>JPQL이 제공하는 모든 검색 조건 제공</b>
    ```java
    member.username.eq("member1") // username = 'member1'
    member.username.ne("member1") //username != 'member1'
    member.username.eq("member1").not() // username != 'member1'
    member.username.isNotNull() //이름이 is not null

    member.age.in(10, 20) // age in (10,20)
    member.age.notIn(10, 20) // age not in (10, 20)
    member.age.between(10,30) //between 10, 30

    member.age.goe(30) // age >= 30
    member.age.gt(30) // age > 30
    member.age.loe(30) // age <= 30
    member.age.lt(30) // age < 30

    member.username.like("member%") //like 검색
    member.username.contains("member") // like ‘%member%’ 검색
    member.username.startsWith("member") //like ‘member%’ 검색
    ```
- <b>AND 조건을 파라미터로 처리</b>
    ```java
    @Test
    void searchAndParam() throws Exception {
        // given
        // when
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        // then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }
    ```
    - `where()`에 파라미터로 검색 조건을 추가하면 `AND` 조건이 추가됨
    - 이 경우 `null` 값은 무시 -> 메소드 추출을 활용해서 동적 쿼리를 깔끔하게 만들 수 있음
___
## 결과 조회
- `fetch()`: 리스트 조회, 데이터 없으면 빈 리스트 반환
- `fetchOne()`: 단 건 조회
    - 결과가 없으면: `null`
    - 결과가 둘 이상이면: `com.querydsl.core.NonUniqueResultException`
- `fetchFirst()`: `limit(1).fetchOne()`
- `fetchResults()`: 페이징 정보 포함, total count 쿼리 추가 실행
- `fetchCount()`: count 쿼리로 변경해서 count 수 조회
    ```java
    @Test
    void resultFetch() throws Exception {
        // given

        // when
        //List
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        //단 건
        Member fetchOne = queryFactory
                .selectFrom(QMember.member)
                .fetchOne();

        //처음 한 건 조회
        Member fetchFirst = queryFactory
                .selectFrom(QMember.member)
                .fetchFirst();

        //페이징에서 사용
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();
        results.getTotal();
        List<Member> content = results.getResults();

        //count 쿼리로 변경
        long total = queryFactory
                .selectFrom(member)
                .fetchCount();

        // then
    }
    ```
___
## 정렬
- <b>정렬</b>
    ```java
    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단, 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    void sort() throws Exception {
        // given
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        // when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        // then
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }
    ```
    - `desc()`: 내림차순 정렬
    - `asc()`: 오름차순 정렬
    - `nullsLast()`, `nullsFirst()`: null 데이터 순서 부여
___
## 페이징
- <b>조회 건수 제한</b>
    ```java
    @Test
    void paging1() throws Exception {
        // given

        // when
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        // then
        assertThat(result.size()).isEqualTo(2);
    }
    ```
- <b>전체 조회 수가 필요하면?</b>
    ```java
    @Test
    void paging2() throws Exception {
        // given

        // when
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        // then
        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }
    ```
> <b>참고</b>
> - 실무에서 페이징 쿼리를 작성할 때, 데이터를 조회하는 쿼리는 여러 테이블을 조인해야 하지만, count 쿼리는 조인이 필요 없는 경우도 있다.
> - 그런데 이렇게 자동화된 count 쿼리는 원본 쿼리와 모두 조인을 해버리기 때문에, 성능이 안나올 수 있다.
> - count 쿼리에 조인이 필요 없는 성능 최적화가 필요하다면, count 전용 쿼리를 별도로 작성해야 한다.
___
## 집합
- <b>집합 함수</b>
    ```java
    @Test
    void aggregation() throws Exception {
        // given

        // when
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);

        // then
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }    
    ```
    - JPQL이 제공하는 모든 집함 함수를 제공한다.
- <b>GroupBy 사용</b>
    ```java
    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    void group() throws Exception {
        // given

        // when
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .orderBy(team.name.asc())
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        // then
        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }
    ```
    - `groupBy`, 그룹화된 결과를 제한하려면 `having`절 사용
- <b>groupBy(), having() 예시</b>
    ```java
    ...
    .groupBy(imte.price)
    .having(item.price.gt(1000))
    ```
___
## 조인 - 기본 조인
<b>기본 조인</b>

조인의 기본 문법은 첫 번째 파라미터에 `조인 대상`을 지정하고, 두 번째 파라미터에 `별칭(alias)으로 사용할 Q 타입`을 지정하면 된다.<br>
```sql
join(조인 대상, 별칭으로 사용할 Q타입)
```
- <b>기본 조인</b>
    ```java
    /**
     * 팀 A에 소속된 모든 회원 찾아라.
     */
    @Test
    void join() throws Exception {
        // given

        // when
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        // then
        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }
    ```
    - `join()`, `innerJoin()`: 내부 조인(inner join)
    - `leftJoin()`: left 외부 조인(left outer join)
    - `rightJoin()`: right 외부 조인(right outer join)
    - JPQL의 `on`과 성능 최적화를 위한 `fetch` 조인 제공
    > <b>참고</b> 
    > - Full outer join은 JPQL에서도 제공하지 않으므로, Querydsl도 제공하지 않는다.
    > - 따라서 Native query를 사용해야 한다.
- <b>세타 조인</b><br>연관관계가 없는 필드로 조인
    ```java
    /**
     * 세타 조인
     * 회원 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    void theta_join() throws Exception {
        // given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        // when
        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .orderBy(member.username.asc())
                .fetch();

        // then
        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }
    ```
    - from 절에 여러 엔티티를 선택해서 세타 조인
    - 외부 조인 불가능 -> 조인 on을 사용하면 가능
___
## 조인 - on절
- ON 절을 활용한 조인(JPA 2.1부터 지원)
    1. 조인 대상 필터링
    2. 연관관계 없는 엔티티 외부 조인
- <b>조인 대상 필터링</b>
    ```java
    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    void join_on_filtering() throws Exception {
        // given
        
        // when
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        // then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }
    ```
- <b>결과</b>
    ```log
    t=[Member(id=3, username=member1, age=10), Team(id=1, name=teamA)]
    t=[Member(id=4, username=member2, age=20), Team(id=1, name=teamA)]
    t=[Member(id=5, username=member3, age=30), null]
    t=[Member(id=6, username=member4, age=40), null]
    ```
> <b>참고</b>
> - `on 절`을 활용해 조인 대상을 필터링할 때, 외부 조인이 아닌 `내부 조인`을 사용하면, `where 절에서 필터링하는 것과 기능이 동일`하다.
> - 따라서, on절을 활용한 조인 대상 필터링을 사용할 때, 내부 조인이면 익숙한 where 절로 해결하고, `정말 외부 조인이 필요한 경우에만 이 기능을 사용`하자.

- <b>연관관계 없는 엔티티 외부 조인</b>
    ```java
    /**
     * 연관관계 없는 엔티티 외부 조인
     * 회원 이름이 팀 이름과 같은 대상 외부 조인
     */
    @Test
    void join_on_no_relation() throws Exception {
        // given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        // when
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team)
                .on(member.username.eq(team.name))
                .orderBy(member.username.asc())
                .fetch();

        // then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }
    ```
    - 하이버네이트 5.1부터 `on`을 사용해서 서로 관계가 없는 필드로 외부 조인하는 기능이 추가되었다. (내부 조인도 물론 가능)
    - `leftJoin()` 부분에 일반 조인과 다르게, `엔티티 하나`만 들어간다.
        - 일반 조인: `leftJoin(member.team, team)`
        - on 조인: `from(member).leftJoin(team).on(xxx)`
- <b>결과</b>
    ```log
    t=[Member(id=3, username=member1, age=10), null]
    t=[Member(id=4, username=member2, age=20), null]
    t=[Member(id=5, username=member3, age=30), null]
    t=[Member(id=6, username=member4, age=40), null]
    t=[Member(id=7, username=teamA, age=0), Team(id=1, name=teamA)]
    t=[Member(id=8, username=teamB, age=0), Team(id=2, name=teamB)]
    ```
___
## 조인 - Fetch join
Fetch 조인은 SQL에서 제공하는 기능이 아니다.<br>
SQL 조인을 활용해서 연관된 엔티티를 SQL 한 번에 조회하는 기능이다.<br>
주로 성능 최적화에 사용하는 방법이다.

- <b>Fetch 조인 미적용</b><br>지연로딩으로 Member, Team SQL 쿼리 각각 실행
    ```java
    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    void fetchJoinNo() throws Exception {
        // given
        em.flush();
        em.clear();

        // when
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        // then
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }
    ```
- <b>Fetch 조인 적용</b><br>즉시로딩으로 Member, Team SQL 쿼리 조인으로 한 번에 조회
    ```java
    @Test
    void fetchJoinUse() throws Exception {
        // given
        em.flush();
        em.clear();

        // when
        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        // then
        assertThat(loaded).as("페치 조인 미적용").isTrue();
    }
    ```
    - 사용 방법
        - `join()`, `leftJoin()` 등 조인 기능 뒤에 `fetchJoin()`이라고 추가만 하면 된다.
___
## 서브 쿼리
`com.querydsl.jpa.JPAExpressions` 사용

- <b>서브 쿼리 eq 사용</b>
    ```java
    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    void subQuery() throws Exception {
        // given
        QMember memberSub = new QMember("memberSub");

        // when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions // static import 가능
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        // then
        assertThat(result)
                .extracting("age")
                .containsExactly(40);
    }
    ```
- <b>서브 쿼리 goe 사용</b>
    ```java
    /**
     * 나이가 평균 이상인 회원 조회
     */
    @Test
    void subQueryGoe() throws Exception {
        // given
        QMember memberSub = new QMember("memberSub");

        // when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions // static import 가능
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        // then
        assertThat(result)
                .extracting("age")
                .containsExactly(30, 40);
    }
    ```
- <b>서브 쿼리 여러 건 처리 in 사용</b>
    ```java
    /**
     * 나이가 10살 초과인 회원 조회
     */
    @Test
    void subQueryIn() throws Exception {
        // given
        QMember memberSub = new QMember("memberSub");

        // when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions // static import 가능
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        // then
        assertThat(result)
                .extracting("age")
                .containsExactly(20, 30, 40);
    }
    ```
- <b>select 절에 서브 쿼리 사용</b>
    ```java
    @Test
    void selectSubQuery() throws Exception {
        // given
        QMember memberSub = new QMember("memberSub");

        // when
        List<Tuple> result = queryFactory
                .select(member.username,
                        JPAExpressions // static import 가능
                                .select(memberSub.age.avg())
                                .from(memberSub)
                )
                .from(member)
                .fetch();

        // then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }
    ```
- <b>static import 활용</b>
    ```java
    import static com.querydsl.jpa.JPAExpressions.select;

    List<Member> result = queryFactory
            .selectFrom(member)
            .where(member.age.eq(
                    select(memberSub.age.max())
                            .from(memberSub)
            ))
            .fetch();
    ```
- <b>from 절의 서브 쿼리 한계</b>
    - JPA JPQL 서브 쿼리의 한계점으로 from 절의 서브 쿼리(인라인 뷰)는 지원하지 않는다. -> 당연히 Querydsl도 지원하지 않는다.
    - 하이버네이트 구현체를 사용하면, select 절의 서브 쿼리는 지원한다.<br>
    -> Querydsl도 하이버네이트 구현체를 사용하면 select 절의 서브 쿼리를 지원한다.
- <b>from 절의 서브 쿼리 해결 방안</b>
    1. 서브 쿼리를 join으로 변경한다. (상황에 따라 가능/불가능)
    2. 애플리케이션에서 쿼리를 2번 분리해서 실행한다.
    3. NativeSQL을 사용한다.
___
## Case 문
<b>select, 조건절(where), order by에서 사용 가능</b>
> <b>참고</b>
> - DB에서는 row 데이터를 필터링, 그룹핑 등으로 최소한의 데이터만 가져오는 작업만 수행하는 것이 좋다.
> - 아래의 예시들과 같이, 어떤 조건에 따라 데이터를 전환/변경/보여주는 쿼리는 지양하도록 하자.

- <b>단순한 조건</b>
    ```java
    @Test
    void basicCase() throws Exception {
        // given

        // when
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        // then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
    ```
- <b>복잡한 조건</b>
    ```java
    @Test
    void complexCase() throws Exception {
        // given

        // when
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        // then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
    ```
- <b>orderBy에서 Case 문 함께 사용하기</b>
    - 예를 들어, 다음과 같은 임의의 순서로 회원을 출력하고 싶다면?
        1. 0 ~ 30살이 아닌 회원을 가장 먼저 출력
        2. 0 ~ 20살 회원 출력
        3. 21 ~ 30살 회원 출력
    ```java
    NumberExpression<Integer> rankPath = new CaseBuilder()
            .when(member.age.between(0, 20)).then(2)
            .when(member.age.between(21, 30)).then(1)
            .otherwise(3);

    List<Tuple> result = queryFactory
            .select(member.username, member.age, rankPath)
            .from(member)
            .orderBy(rankPath.desc())
            .fetch();

    for (Tuple tuple : result) {
        String username = tuple.get(member.username);
        Integer age = tuple.get(member.age);
        Integer rank = tuple.get(rankPath);
        System.out.println("username = " + username + " age = " + age + " rank = " + rank);
    }
    ```
    - Querydsl은 자바 코드로 작성하기 때문에, `rankPath`처럼 복잡한 조건을 변수로 선언해서 `select`절, `orderBy`절에서 함께 사용할 수 있다.
- <b>결과</b>
    ```log
    결과
    username = member4 age = 40 rank = 3
    username = member1 age = 10 rank = 2
    username = member2 age = 20 rank = 2
    username = member3 age = 30 rank = 1
    ```
___
## 상수, 문자 더하기
상수가 필요하면 `Expressions.constant(xxx)` 사용
- <b>상수</b>
    ```java
    @Test
    void constant() throws Exception {
        // given

        // when
        List<Tuple> result = queryFactory.
                select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        // then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }
    ```
> <b>참고</b>
> - 위와 같이 최적화가 가능하면, SQL에 constant 값을 넘기지 않는다.
> - 상수를 더하는 것처럼 최적화가 어려우면 SQL에 constant 값을 넘긴다.

- <b>문자 더하기 oncact</b>
    ```java
    @Test
    void concat() throws Exception {
        // given

        // when
        //{username}_{age}
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        // then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
    ```
    - 결과: member1_10
> <b>참고</b>
> - `member.age.stringValue()` 부분이 중요한데, 문자가 아닌 다른 타입들은 `stringValue()`로 문자로 변환할 수 있다.
> - 이 방법은 `ENUM을 처리할 때도 자주 사용`된다.
