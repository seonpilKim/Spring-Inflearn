#  API 개발 기본
## 목차
- 회원 등록 API
- 회원 수정 API
- 회원 조회 API
- 조회용 샘플 데이터 입력
___
## 회원 등록 API
### V1 엔티티를 Request Body에 직접 매핑
```java
@RestController
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    @PostMapping("/api/v1/members")
    public CreateMemberResponse saveMemberV1(@RequestBody @Validated Member member) {
        Long memberId = memberService.join(member);
        return new CreateMemberResponse(memberId);
    }

    @AllArgsConstructor
    @Getter
    static class CreateMemberResponse {
        private Long id;
    }
}
```
- <b>문제점</b>
    - 엔티티에 프레젠테이션 계층을 위한 로직이 추가된다.
    - 엔티티에 API 검증을 위한 로직이 들어간다. (@NotEmpty 등)
    - 실무에서는 회원 엔티티를 위한 API가 다양하게 만들어지는데, 한 엔티티에 각각의 API를 위한 모든 요청 요구사항을 담기는 어렵다.
    - 엔티티가 변경되면 API 스펙이 변한다.
- <b>결론</b>
    - API 요청 스펙에 맞추어 별도의 DTO를 파라미터로 받도록 하자.
### V2 엔티티 대신에 DTO를 Request에 매핑
```java
@RestController
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    @PostMapping("/api/v2/members")
    public CreateMemberResponse saveMemberV2(@RequestBody @Validated CreateMemberRequest request) {
        Member member = new Member(request.getName());
        Long memberId = memberService.join(member);

        return new CreateMemberResponse(memberId);
    }

    @AllArgsConstructor
    @Getter
    static class CreateMemberResponse {
        private Long id;
    }

    @Getter
    static class CreateMemberRequest {
        @NotEmpty
        private String name;
    }
}
```
- `CreateMemberRequest`를 `Member` 엔티티 대신 RequestBody와 매핑한다.
- 엔티티와 프레젠테이션 계층을 위한 로직을 분리할 수 있다.
- 엔티티와 API 스펙을 명확하게 분리할 수 있다.
- `엔티티가 변해도 API 스펙이 변하지 않는다.`
- 실무에서는 엔티티를 API 스펙에 노출하면 안된다!
> <b>참고</b> (https://jojoldu.tistory.com/407)
> - Jackson은 POST 요청 시, Jackson2HttpMessageConverter가 데이터를 처리하는데, 내부적으로 ObjectMapper를 사용한다.
> - ObjectMapper는 `기본 생성자`와 `getter` 또는 `setter` 또는 `public field` 중 하나를 보고 property 명을 알아낸다.
> - 그러므로, `기본 생성자`와 `getter`만 있어도 property 명을 알아내어 값을 주입시켜 줄 수 있다.
> - ⭐영한님은 DTO 정도는 `@Data`를 적용해도 상관없다고 하신다.
___
## 회원 수정 API
```java
@RestController
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    @PutMapping("/api/v2/members/{id}")
    public UpdateMemberResponse updateMemberV2(
            @PathVariable("id") Long memberId,
            @RequestBody @Validated UpdateMemberRequest request) {
        memberService.update(memberId, request.getName()); // 변경 감지
        Member findMember = memberService.findOne(memberId);
        return new UpdateMemberResponse(findMember.getId(), findMember.getName());
    }

    @AllArgsConstructor
    @Getter
    static class UpdateMemberResponse {
        private Long id;
        private String name;
    }

    @Getter
    static class UpdateMemberRequest{
        private String name;
    }
}
```
- 회원 수정도 DTO를 요청 파라미터에 매핑
> <b>참고</b>
> - PUT은 전체 업데이트를 할 때 사용해야 한다.
> - 부분 업데이트를 하려면 PATCH 혹은 POST를 사용해야 한다.
___
## 회원 조회 API
### 회원 조회 V1: 응답 값으로 엔티티를 직접 외부에 노출
```java
@RestController
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    @GetMapping("/api/v1/members")
    public List<Member> membersV1() {
        return memberService.findMembers();
    }
}
```
- <b>문제점</b>
    - 엔티티에 프레젠테이션 계층을 위한 로직이 추가된다.
    - 기본적으로 엔티티의 모든 값이 노출된다.
    - 응답 스펙을 맞추기 위해 로직이 추가된다. (@JsonIgnore, 별도의 뷰 로직 등)
    - 실무에서는 같은 엔티티에 대해 API가 용도에 따라 다양하게 만들어지는데, 한 엔티티에 각각의 API를 위한 프레젠테이션 응답 로직을 담기는 어렵다.
    - 엔티티가 변경되면 API 스펙이 변한다.
    - 추가로 컬렉션을 직접 반환하면, 향후 API 스펙을 변경하기 어렵다. (별도의 Result 클래스 생성으로 해결)
- <b>결론</b>
    - API 응답 스펙에 맞추어 별도의 DTO를 반환하자.
### 회원 조회 V2: 응답 값으로 엔티티가 아닌 별도의 DTO 사용
```java
@RestController
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    @GetMapping("/api/v2/members")
    public Result membersV2() {
        List<MemberDTO> collect = memberService.findMembers().stream()
                .map(m -> new MemberDTO(m.getName()))
                .collect(Collectors.toList());

        return new Result(collect.size(), collect);
    }

    @Getter
    @AllArgsConstructor
    static class Result<T> {
        private int count;
        private T data;
    }

    @Getter
    @AllArgsConstructor
    static class MemberDTO {
        private String name;
    }
}
```
- 엔티티를 DTO로 변환해서 반환한다.
- 엔티티가 변해도 API 스펙이 변경되지 않는다.
- 추가로 `Result` 클래스로 컬렉션을 감싸서 향후 필요한 필드를 추가할 수 있다.
___
## 조회용 샘플 데이터 입력
API 개발 고급 설명을 위해 샘플 데이터를 입력하자.
- userA
    - JPA1 BOOK
    - JPA2 BOOK
- userB
    - SPRING1 BOOK
    - SPRING2 BOOK
```java
@Component
@RequiredArgsConstructor
public class InitDb {

    private final InitService initService;

    @PostConstruct
    public void init() {
        initService.dbInit1();
        initService.dbInit2();
    }

    @Component
    @Transactional
    @RequiredArgsConstructor
    static class InitService {
        private final EntityManager em;

        public void dbInit1() {
            Member member = createMember("userA", "서울", "1", "1111");
            em.persist(member);

            Book book = new Book("JPA1 BOOK", 10000, 100, "김영한", "1234");
            Book book2 = new Book("JPA2 BOOK", 20000, 200, "토비", "5678");
            em.persist(book);
            em.persist(book2);

            OrderItem orderItem = OrderItem.createOrderItem(book, 10000, 1);
            OrderItem orderItem2 = OrderItem.createOrderItem(book2, 20000, 2);

            Delivery delivery = new Delivery(member.getAddress());

            Order order = Order.createOrder(member, delivery, orderItem, orderItem2);
            em.persist(order);
        }

        public void dbInit2() {
            Member member = createMember("userB", "부산", "2", "2222");
            em.persist(member);

            Book book = new Book("SPRING1 BOOK", 15000, 150, "김영한", "22-1234");
            Book book2 = new Book("SPRING2 BOOK", 25000, 250, "토비", "22-5678");
            em.persist(book);
            em.persist(book2);

            OrderItem orderItem = OrderItem.createOrderItem(book, 15000, 3);
            OrderItem orderItem2 = OrderItem.createOrderItem(book2, 25000, 4);

            Delivery delivery = new Delivery(member.getAddress());

            Order order = Order.createOrder(member, delivery, orderItem, orderItem2);
            em.persist(order);
        }

        private Member createMember(String name, String city, String street, String zipcode) {
            Member member = new Member(name);
            member.updateAddress(city, street, zipcode);
            return member;
        }
    }
}
```
> <b>참고</b>
> - 스프링 라이프 사이클 때문에, @PostConstruct에 @Transactional을 설정할 수 없다.
> - 따라서, 로직을 별도의 Bean으로 등록해서 사용하였다.