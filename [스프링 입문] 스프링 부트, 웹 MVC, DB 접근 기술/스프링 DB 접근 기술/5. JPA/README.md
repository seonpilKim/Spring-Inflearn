# 스프링 DB 접근 기술
- **스프링 데이터 접근**
    1. H2 데이터베이스 설치
    2. 순수 Jdbc
    3. 스프링 통합 테스트
    4. 스프링 JdbcTemplate
    5. **JPA**
    6. 스프링 데이터 JPA

## JPA
- `JPA`는 기존의 반복 코드는 물론이고, 기본적인 SQL도 JAP가 직접 만들어서 실행해준다.
- `JPA`를 사용하면, SQL과 데이터 중심의 설계에서 `객체 중심의 설계`로 패러다임 전환이 가능하다.
- `JPA`를 사용하면 개발 생산성을 크게 높일 수 있다.

- **build.gradle 파일에 JPA, h2 데이터베이스 관련 라이브러리 추가**
```gradle
dependencies {
    ...
//  implementation 'org.springframework.boot:spring-boot-starter-jdbc'
	implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
	...
}
```
- `spring-boot-starter-data-jap`는 내부에 jdbc 관련 라이브러리를 포함하므로, jdbc는 제거해도 된다.

- **스프링 부트에 JPA 설정 추가**
```properties
...
spring.jpa.show-sql=true
spring.jpa.hibernate.ddl-auto=none
```
- `show-sql` : JPA가 생성하는 SQL을 출력한다.
- `ddl-auto` : JPA는 테이블을 자동으로 생성하는 기능을 제공하는데, `none`을 사용하면 해당 기능을 끌 수 있다.
    - `create`를 사용하면 엔티티 정보를 바탕으로 테이블도 직접 생성해준다.

- **JPA 엔티티 매핑**
```java
package hello.hellospring.domain;

import javax.persistence.*;

@Entity
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    ...
}
```

- **JPA 회원 리포지토리**
```java
package hello.hellospring.repository;

import hello.hellospring.domain.Member;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

public class JpaMemberRepository implements MemberRepository{

    private final EntityManager em;

    public JpaMemberRepository(EntityManager em) {
        this.em = em;
    }

    @Override
    public Member save(Member member) {
        em.persist(member);
        return member;
    }

    @Override
    public Optional<Member> findById(Long id) {
        Member member = em.find(Member.class, id);
        return Optional.ofNullable(member);
    }

    @Override
    public Optional<Member> findByName(String name) {
        List<Member> result = em.createQuery("select m from Member m where m.name = :name", Member.class)
                .setParameter("name", name)
                .getResultList();
        return result.stream().findAny();
    }

    @Override
    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }
}
```

- **서비스 계층에 트랜잭션 추가**
```java
import org.springframework.transaction.annotation.Transactional

@Transactional
public class MemberService { ... }
```
- `org.springframework.transaction.annotation.Transactional`를 사용하자.
- 스프링은 해당 클레스의 메소드를 실행할 때 트랜잭션을 시작하고, 메소드가 정상 종료되면 트랙잭션을 commit한다.
    - 만약 런타임 예외가 발생하면 rollback한다.
- `JPA를 통한 모든 데이터 변경은 트랜잭션 안에서 실행해야 한다.`

- **JPA를 사용하도록 스프링 설정 변경**
```java
@Configuration
public class SpringConfig {

    private EntityManager em;

    public SpringConfig(EntityManager em) {
        this.em = em;
    }

    ...

    @Bean
    public MemberRepository memberRepository(){
        // return new MemoryMemberRepository();
        // return new JdbcMemberRepository(dataSource);
        // return new JdbcTemplateMemberRepository(dataSource);
        return new JpaMemberRepository(em);
    }
}
```