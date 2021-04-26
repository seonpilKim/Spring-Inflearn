# 스프링 DB 접근 기술
- **스프링 데이터 접근**
    1. H2 데이터베이스 설치
    2. 순수 Jdbc
    3. **스프링 통합 테스트**
    4. 스프링 JdbcTemplate
    5. JPA
    6. 스프링 데이터 JPA

## 스프링 통합 테스트
- 스프링 컨테이너와 DB까지 연결한 통합 테스트를 진행해본다.
- **회원 서비스 스프링 통합 테스트**
```java
package hello.hellospring.service;

import hello.hellospring.domain.Member;
import hello.hellospring.repository.MemberRepository;
import hello.hellospring.repository.MemoryMemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
public class MemberServiceIntegrationTest {

    // Test는 다른 곳에서 사용하는 것이 아닌, 여기서만 사용하므로 필드수준에서 autowired 받는 것이 편하다
    @Autowired MemberService memberService;
    @Autowired MemberRepository memberRepository;

    @Test
    void 회원가입() {
        //given
        Member member = new Member();
        member.setName("Hello!");

        //when
        Long saveId = memberService.join(member);

        //then
        Member findMember = memberService.findOne(saveId).get();
        assertThat(member.getName()).isEqualTo(findMember.getName());
    }

    @Test
    public void 중복_회원_예외() {
        //given
        Member member1 = new Member();
        member1.setName("hi");

        Member member2 = new Member();
        member2.setName("hi");

        //when
        memberService.join(member1);
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> memberService.join(member2));
        assertThat(e.getMessage()).isEqualTo("이미 존재하는 회원입니다.");

        //then
    }
}
```
- `@SpringBootTest` : 스프링 컨테이너와 테스트를 함께 실행한다.
- `@Transactional` : 테스트 케이스에 이 애노테이션이 있으면, 테스트 시작 전에 트랜잭션을 시작하고, 테스트 완료 후에 항상 롤백한다.
    - 이렇게 하면 DB에 데이터가 남지 않으므로, 다음 테스트에 영향을 주지 않는다.