# API 개발 고급 - 지연 로딩과 조회 성능 최적화
- 주문 + 배송정보 + 회원을 조회하는 API를 만들자.
- 지연 로딩 때문에 발생하는 성능 문제를 단계적으로 해결해보자.
## 목차
- 간단한 주문 조회 V1: 엔티티를 직접 노출
- 간단한 주문 조회 V2: 엔티티를 DTO로 변환
- 간단한 주문 조회 V3: 엔티티를 DTO로 변환 - 페치 조인 최적화
- 간단한 주문 조회 V4: JPA에서 DTO로 바로 조회
- 정리
___
## 간단한 주문 조회 V1: 엔티티를 직접 노출
```java

/**
 * xToOne(ManyToOne, OneToOne) 성능 최적화
 * Order
 * Order -> Member
 * Order -> Delivery
 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderRepository orderRepository;

    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch("", OrderStatus.ORDER));
        for (Order order : orders) {
            order.getMember().getName();
            order.getDelivery().getAddress();
        }
        return orders;
    }
}
```
- 엔티티를 직접 노출하는 것은 좋지 않다.
- `order` -> `member`와 `order` -> `address`는 지연 로딩이다.
    - 따라서 실제 엔티티 대신에 프록시가 존재한다.
    - jackson 라이브러리는 기본적으로 이 프록시 객체를 json으로 어떻게 생성해야 하는지 모른다. -> 예외 발생❗
    - `Hibernate5Module`을 스프링 빈으로 등록하면 해결(스프링 부트 사용중)
### Hibernate5Module 등록
`JpashopApplication`에 다음 코드를 추가하자.
```java
@Bean
Hibernate5Module hibernate5Module() {
	return new Hibernate5Module();
}
```
- 기본적으로 초기화 된 프록시 객체만 노출, 초기화 되지 않은 프록시 객체는 노출 안함
> <b>참고</b>
> - `build.gradle`에 다음 라이브러리를 추가하자.
> ```gradle
> implementation 'com.fasterxml.jackson.datatype:jackson-datatype-hibernate5'
> ```
### Hibernate5Module 강제 지연 로딩
```java
@Bean
Hibernate5Module hibernate5Module() {
 Hibernate5Module hibernate5Module = new Hibernate5Module();
    //강제 지연 로딩 설정
    hibernate5Module.configure(Hibernate5Module.Feature.FORCE_LAZY_LOADING, true);
    return hibernate5Module;
}
```
- 이 옵션을 키면 `order -> member`, `member -> orders` 양방향 연관관계를 계속 로딩하게 된다.
    - 따라서 `@JsonIgnore` 옵션을 한 곳에 적용해야 한다.
> <b>주의</b>
> - 엔티티를 직접 노출할 때는 양방향 연관관계가 걸린 곳 중 한 곳을 꼭 `@JsonIgnore` 처리 해야 한다.
> - 그러지 않으면, 양쪽을 서로 호출하면서 무한 루프에 빠진다.

> <b>참고</b>
> - 앞에서 계속 강조했듯이, 엔티티를 API 응답으로 외부로 노출하는 것은 좋지 않다.
> - 따라서, `Hibernate5Module`을 사용하기 보다는 `DTO로 변환해서 반환`하는 것이 더 좋은 방법이다.
___
## 간단한 주문 조회 V2: 엔티티를 DTO로 변환
```java

/**
 * xToOne(ManyToOne, OneToOne) 성능 최적화
 * Order
 * Order -> Member
 * Order -> Delivery
 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderRepository orderRepository;

    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> ordersV2() {
        return orderRepository.findAllByString(new OrderSearch("", OrderStatus.ORDER)).stream()
                .map(SimpleOrderDto::new)
                .collect(toList());
    }

    @Getter
    static class SimpleOrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;

        public SimpleOrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getOrderStatus();
            address = order.getDelivery().getAddress();
        }
    }
}
```
- 엔티티를 DTO로 변환하는 일반적인 방법이다.
- 쿼리가 총 `1 + N + N`번 실행된다. (v1과 쿼리 수 결과는 같다.)
    - `order` 조회 1번 (order 조회 결과 수가 N이 된다.)
    - `order -> member` 지연 로딩 조회 N번
    - `order -> delivery` 지연 로딩 조회 N번
    > ex) order의 결과가 4개면 최악의 경우, 1 + 4 + 4 번 실행된다.
    >   - 지연 로딩은 영속성 컨텍스트에서 조회하므로, 이미 조회된 경우 쿼리를 생략한다.
___
## 간단한 주문 조회 V3: 엔티티를 DTO로 변환 - 페치 조인 최적화
```java

/**
 * xToOne(ManyToOne, OneToOne) 성능 최적화
 * Order
 * Order -> Member
 * Order -> Delivery
 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderRepository orderRepository;

    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> ordersV3() {
        return orderRepository.findAllWithMemberDelivery().stream()
                .map(SimpleOrderDto::new)
                .collect(toList());
    }

    @Getter
    static class SimpleOrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;

        public SimpleOrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getOrderStatus();
            address = order.getDelivery().getAddress();
        }
    }
}
```
### OrderRepository - 추가 코드
```java
public List<Order> findAllWithMemberDelivery() {
    return em.createQuery(
            "select o from Order o " +
                    "join fetch o.member m " +
                    "join fetch o.delivery d", Order.class)
            .getResultList();
}
```
- 엔티티를 페치 조인(fetch join)을 사용해서 쿼리 1번에 조회
- 페치 조인으로 `order -> member`, `order -> delivery`는 이미 조회가 된 상태이므로, 지연로딩❌
___
## 간단한 주문 조회 V4: JPA에서 DTO로 바로 조회
### OrderSimpleApiController - 추가
```java
private final OrderSimpleQuerytRepository orderSimpleQueryRepository; // 의존관계 주입
@GetMapping("/api/v4/simple-orders")
public List<OrderSimpleQueryDto> ordersV4() {
    return orderSimpleQueryRepository.findOrderDtos();
}
```
### OrderSimpleQueryRepository 조회 전용 리포지토리
```java
@Repository
@RequiredArgsConstructor
public class OrderSimpleQueryRepository {

    private final EntityManager em;

    public List<OrderSimpleQueryDto> findOrderDtos() {
        return em.createQuery(
                "select new jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDto(o.id, o.member.name, o.orderDate, o.orderStatus, o.delivery.address) " +
                        "from Order o " +
                        "join o.member m " +
                        "join o.delivery d", OrderSimpleQueryDto.class)
                .getResultList();
    }
}
```
### OrderSimpleQueryDto 리포지토리에서 DTO 직접 조회
```java
@Getter
public class OrderSimpleQueryDto {
    private Long orderId;
    private String name;
    private LocalDateTime orderDate;
    private OrderStatus orderStatus;
    private Address address;

    public OrderSimpleQueryDto(Long orderId, String name, LocalDateTime orderDate, OrderStatus orderStatus, Address address) {
        this.orderId = orderId;
        this.name = name; //LAZY 초기화
        this.orderDate = orderDate;
        this.orderStatus = orderStatus;
        this.address = address; //LAZY 초기화
    }
}
```
- 일반적인 SQL을 사용할 때 처럼 원하는 값을 선택해서 조회
- `new` 명령어를 사용해서 JPQL의 결과를 DTO로 즉시 변환
- SELECT절에서 원하는 데이터를 직접 선택하므로 DB -> 애플리케이션 네트웤 용량 최적화(생각보다 미비)
- 리포지토리 재사용성 떨어짐, API 스펙에 맞춘 코드가 리포지토리에 들어가는 단점
## 정리
- 엔티티를 DTO로 변환하거나, DTO로 바로 조회하는 두 가지 방법은 각각 장단점이 존재한다.
    - 둘 중 상황에 따라 더 나은 방법을 선택하자.
    - 엔티티로 조회하면 리포지토리 재사용성도 좋고, 개발도 단순하다.
### 쿼리 방식 선택 권장 순서
1. 우선 엔티티를 조회해서 DTO로 변환하는 방법을 선택
2. 필요하면 페치 조인으로 성능 최적화 -> 대부분 이슈 해결 가능
3. 그래도 안되면, DTO로 직접 조회
4. 최후의 방법은 JPA가 제공하는 네이티브 SQL이나, 스프링 JDBC Template을 사용해서 SQL을 직접 사용