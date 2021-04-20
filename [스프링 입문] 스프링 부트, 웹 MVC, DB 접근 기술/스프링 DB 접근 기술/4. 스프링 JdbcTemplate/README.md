# 스프링 DB 접근 기술
- **스프링 데이터 접근**
    1. H2 데이터베이스 설치
    2. 순수 Jdbc
    3. 스프링 통합 테스트
    4. **스프링 JdbcTemplate**
    5. JPA
    6. 스프링 데이터 JPA

## 스프링 JdbcTemplate
- 순수 Jdbc와 동일하게 환경설정을 하면 된다.
- `스프링 JdbcTemplate`과 `MyBatis`같은 라이브러리는 JDBC API에서 보았던 반복 코드를 대부분 제거해준다.
    - 그러나 SQL은 직접 작성해야 한다.

- **스프링 JdbcTemplate 회원 리포지토리**
```java
package hello.hellospring.repository;

import hello.hellospring.domain.Member;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JdbcTemplateMemberRepository implements MemberRepository{

    private final JdbcTemplate jdbcTemplate;

    // 생성자가 하나 뿐이므로, Autowired 생략 가능
    public JdbcTemplateMemberRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Member save(Member member) {
        SimpleJdbcInsert jdbcInsert = new SimpleJdbcInsert(jdbcTemplate);
        jdbcInsert.withTableName("member").usingGeneratedKeyColumns("id");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", member.getName());

        Number key = jdbcInsert.executeAndReturnKey(new MapSqlParameterSource(parameters));
        member.setId(key.longValue());
        return member;
    }

    @Override
    public Optional<Member> findById(Long id) {
        List<Member> query = jdbcTemplate.query("select * from member where id = ?", memberRowMapper(), id);
        // stream의 findAny 메소드로 반환시, 결과가 없을 것도 고려하여 반환형을 Optional로 감싸주어야 한다.
        return query.stream().findAny();
    }

    @Override
    public Optional<Member> findByName(String name) {
        List<Member> query = jdbcTemplate.query("select * from member where name = ?", memberRowMapper(), name);
        return query.stream().findAny();
    }

    @Override
    public List<Member> findAll() {
        return jdbcTemplate.query("select * from member", memberRowMapper());
    }

    private RowMapper<Member> memberRowMapper(){
        return (rs, rowNum) -> {
            Member member = new Member();
            member.setId(rs.getLong("id"));
            member.setName(rs.getString("name"));
            return member;
        };
    }
}
```
- **JdbcTemplate을 사용하도록 스프링 설정 변경**
```java
@Configuration
public class SpringConfig {

    private DataSource dataSource;

    ...
    
    @Bean
    public MemberRepository memberRepository(){
        // return new MemoryMemberRepository();
        // return new JdbcMemberRepository(dataSource);
        return new JdbcTemplateMemberRepository(dataSource);
    }
}

```