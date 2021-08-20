# 회원 도메인 개발
- <b>구현 기능</b>
    - 회원 등록
    - 회원 목록 조회
## 목차
- 회원 리포지토리 개발
- 회원 서비스 개발
- 회원 기능 테스트
___
## 회원 리포지토리 개발
### 회원 리포지토리 코드
```java
@Repository
@RequiredArgsConstructor
public class MemberRepository {

    private final EntityManager em;

    public void save(Member member) {
        em.persist(member);
    }

    public Member findOne(Long id) {
        return em.find(Member.class, id);
    }

    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    public List<Member> findByName(String name) {
        return em.createQuery("select m from Member m where m.name = :name", Member.class)
                .setParameter("name", name)
                .getResultList();
    }
}
```
- <b>기술 설명</b>
    - `@Repository`: 스프링 빈으로 등록, JPA 예외를 스프링 기반 예외로 예외 변환
    - `@PersistenceContext`: 엔티티 매니저(`EntityManager`) 주입
    - `@PersistenceUnit`: 엔티티 매니저 팩토리(`EntityMangerFactory`) 주입
    - `@RequriedArgsConstructor`: Lombok 지원, 엔티티 매니저 자동 주입
- <b>기능 설명</b>
    - `save()`
    - `findOne()`
    - `findAll()`
    - `findByName()`
___
## 회원 서비스 개발
### 회원 서비스 코드
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    @Transactional
    public Long join(Member member) {
        validateDuplicateMember(member);
        memberRepository.save(member);
        return member.getId();
    }

    public List<Member> findMembers() {
        return memberRepository.findAll();
    }

    public Member findOne(Long memberId) {
        return memberRepository.findOne(memberId);
    }

    private void validateDuplicateMember(Member member) {
        List<Member> findMembers = memberRepository.findByName(member.getName());
        if (!findMembers.isEmpty()) {
            throw new IllegalStateException("이미 존재하는 회원입니다.");
        }
    }
}
```
- <b>기술 설명</b>
    - `@Service`: 스프링 빈으로 등록
    - `@Transactional`: 트랜잭션, 영속성 컨텍스트
        - `readOnly=true`: 데이터의 변경이 없는 읽기 전용 메소드에 사용, 영속성 컨텍스트를 플러시하지 않으므로, 약간의 성능 향상
        - 데이터베이스 드라이버가 지원하면 DB에서 성능 향상
    - `@RequriedArgsConstructor`: Lombok 지원, 회원 리포지토리 자동 주입
- <b>기능 설명</b>
    - `join()`
    - `findMembers()`
    - `findOne()`
> <b>참고</b>
> - 실무에서는 검증 로직이 있어도, 멀티 스레드 상황을 고려해서 회원 테이블의 회원명 컬럼에 `유니크 제약 조건`을 추가하는 것이 안전하다.
___
## 회원 기능 테스트
- <b>테스트 요구사항</b>
    - 회원가입을 성공해야 한다.
    - 회원가입할 때, 같은 이름이 있으면 예외가 발생해야 한다.
### 회원가입 테스트 코드
```java
@SpringBootTest
@Transactional
class MemberServiceTest {

    @Autowired
    MemberService memberService;

    @Autowired
    MemberRepository memberRepository;
    
    @Test
    @Rollback(value = false)
    void 회원가입() throws Exception {
        // given
        Member member = new Member("memberA");
        member.updateAddress("bosung", "boknae", "25-2");

        // when
        memberService.join(member);
        Member findMember = memberService.findOne(member.getId());

        // then
        assertThat(findMember).isEqualTo(member);
    }
    
    @Test
    void 중복_회원_예외() throws Exception {
        // given
        Member member = new Member("memberA");

        Member member2 = new Member("memberA");

        // when
        memberService.join(member);
        
        // then
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> memberService.join(member2));
        assertEquals("이미 존재하는 회원입니다.", thrown.getMessage());
    }
}
```
- <b>기술 설명</b>
    - `@SpringBootTest`: 스프링 부트를 띄워 통합 테스트 진행
        - 이게 없으면 `@Autowired` 지원❌
        - `Junit5`부터 `@RunWith(SpringRunner.class)` 자동 포함
    - `@Transactional`
        - 반복 가능한 테스트 지원
        - 각각의 테스트를 실행할 때마다, 트랜잭션을 시작하고 `테스트가 끝나면 트랜잭션을 강제로 롤백`
            - 이 애노테이션이 테스트 케이스에서 사용될 때만 롤백
- <b>기능 설명</b>        
    - 회원가입 테스트
    - 중복 회원 예외처리 테스트
> <b>참고</b>
> - 테스트 케이스 작성: Given, When, Then
> - http://martinfowler.com/bliki/GivenWhenThen.html
> - 이 방법이 필수는 아니지만, 이 방법을 기본으로 다양하게 응용하는 것을 권장
- <b>테스트 케이스를 위한 설정</b>
    - 테스트 케이스는 격리된 환경에서 실행하고, 끝나면 데이터를 초기화하는 것이 좋다.
        - 그런 면에서 `메모리 DB`를 사용하는 것이 가장 이상적이다.
    - 추가로 테스트 케이스를 위한 스프링 환경과, 일반적으로 애플리케이션을 실행하는 환경은 보통 다르므로, 설정 파일을 다르게 사용하자.
    - 다음과 같이 간단하게 테스트용 설정 파일을 추가하자.
        - `test/resources/application.properties`
        - 스프링 부트는 `spring.datasource` 설정이 없으면, 기본적으로 `메모리 DB`를 사용하고, driver-class도 현재 등록된 라이브러리를 보고 찾아준다.
        - 추가로 `ddl-auto`도 `create-drop`모드로 동작한다.
            - 따라서 데이터 소스나, JPA관련 별도의 추가 설정을 하지 않아도 된다.
        - SQL 출력 결과를 보기 위해 JPA, Log 설정 정도만 추가해주자.