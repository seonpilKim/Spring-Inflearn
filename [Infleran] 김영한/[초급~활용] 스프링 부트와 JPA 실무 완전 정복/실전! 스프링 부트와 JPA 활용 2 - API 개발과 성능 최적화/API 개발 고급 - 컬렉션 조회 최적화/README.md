# API 개발 고급 - 컬렉션 조회 최적화
## 목차
- 주문 조회 V1: 엔티티 직접 노출
- 주문 조회 V2: 엔티티를 DTO로 변환
- 주문 조회 V3: 엔티티를 DTO로 변환 - 페치 조인 최적화
- 주문 조회 V3.1: 엔티티를 DTO로 변환 - 페이징과 한계 돌파
- 주문 조회 V4: JPA에서 DTO 직접 조회
- 주문 조회 V5: JPA에서 DTO 직접 조회 - 컬렉션 조회 최적화
- 주문 조회 V6: JPA에서 DTO로 직접 조회 - 플랫 데이터 최적화
- 정리
___
## 주문 조회 V1: 엔티티 직접 노출
```java
@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;

    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch("", OrderStatus.ORDER));
        for (Order order : orders) {
            order.getMember().getName();
            order.getDelivery().getAddress();
            List<OrderItem> orderItems = order.getOrderItems();
            orderItems.stream().forEach(o -> o.getItem().getName());
        }

        return orders;
    }
}
```
- `orderItem`, `item` 관계를 직접 초기화하면 `Hibernate5Module` 설정에 의해 엔티티를 JSON으로 생성한다.
- 양방향 연관관계면 무한 루프에 걸리지 않게 한 곳에 `@JsonIgnore`를 추가해야 한다
- 엔티티를 직접 노출하므로, 좋은 방법이 아니다.
___
## 주문 조회 V2: 엔티티를 DTO로 변환
```java
@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;

    @GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2() {
        return orderRepository.findAllByString(new OrderSearch("", OrderStatus.ORDER)).stream()
                .map(o -> new OrderDto(o))
                .collect(Collectors.toList());
    }

    @Getter
    static class OrderDto {

        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems;

        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getOrderStatus();
            address = order.getDelivery().getAddress();
            orderItems = order.getOrderItems().stream()
                    .map(orderItem -> new OrderItemDto(orderItem))
                    .collect(Collectors.toList());
        }
    }

    @Getter
    static class OrderItemDto {

        private String itemName;
        private int orderPrice;
        private int count;

        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();;
        }
    }
}
```
- 지연 로딩으로 너무 많은 SQL 실행
- SQL 실행 수
    - `order` 1번
    - `member`, `address` N번(order 조회 수 만큼)
    - `orderItem` N번(order 조회 수 만큼)
    - `item` N번(orderItem 조회 수 만큼)
> <b>참고</b>
> - 지연 로딩은 영속성 컨텍스트에 있으면, 영속성 컨텍스트에 있는 엔티티를 사용하고, 없으면 SQL을 실행한다.
> - 따라서 같은 영속성 컨텍스트에서 이미 로딩한 회원 엔티티를 추가로 조회하면 SQL을 실행하지 않는다.
___
## 주문 조회 V3: 엔티티를 DTO로 변환 - 페치 조인 최적화
```java
@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;

    @GetMapping("/api/v3/orders")
    public List<OrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithItem();

        return orders.stream()
                .map(OrderDto::new)
                .collect(Collectors.toList());
    }
    @Getter
    static class OrderDto {

        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems;

        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getOrderStatus();
            address = order.getDelivery().getAddress();
            orderItems = order.getOrderItems().stream()
                    .map(orderItem -> new OrderItemDto(orderItem))
                    .collect(Collectors.toList());
        }
    }

    @Getter
    static class OrderItemDto {

        private String itemName;
        private int orderPrice;
        private int count;

        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();;
        }
    }
}
```
### OrderRepository에 추가
```java
public List<Order> findAllWithItem() {
    return em.createQuery(
            "select distinct o from Order o " +
                    "join fetch o.member m " +
                    "join fetch o.delivery d " +
                    "join fetch o.orderItems oi " +
                    "join fetch oi.item i", Order.class
    ).getResultList();
}
```
- 페치 조인으로 SQL이 1번만 실행됨
- `distinct`를 사용한 이유는, 1:N 조인이 있으므로 데이터베이스 row가 증가한다.
    - 그 결과, 같은 order 엔티티의 조회 수도 증가하게 된다.
    - JPA의 distinct는 SQL에 distinct를 추가하고, 또한 같은 엔티티가 조회될 때 애플리케이션에서 중복을 걸러준다.
    - 이 예에서 order가 컬렉션 페치 조인 때문에 중복 조회 되는 것을 막아준다.
- <b>단점</b>
    - 페이징❌
> <b>참고</b>
> - 컬렉션 페치 조인을 사용하면 페이징이 불가능하다.
> - 하이버네이트는 경고 로그를 남기면서, 모든 데이터를 DB에서 읽어오고, 메모리에서 페이징 해버린다.(매우 위험❗)

> <b>참고</b>
> - 컬렉션 페치 조인은 1개만 사용할 수 있다.
> - 컬렉션 둘 이상에 페치 조인을 사용하면 안 된다.
>   - 데이터가 부정합하게 조회될 수 있음❗
___
## 주문 조회 V3.1: 엔티티를 DTO로 변환 - 페이징과 한계 돌파
### 페이징 불가
- 컬렉션을 페치 조인하면 페이징이 불가능하다.
    - 컬렉션을 페치 조인하면 일대다 조인이 발생하므로, 데이터가 예측할 수 없이 증가한다.
    - 일대다에서 일(1)을 기준으로 하는 페이징을 하는 것이 목적이다.
        - 그런데 데이터는 다(N)를 기준으로 row가 생성된다.
    - Order를 기준으로 페이징하고 싶은데, 다(N)인 OrderItem을 조인하면 OrderItem이 기준이 되어버린다.
- 이 경우 하이버네이트는 경고 로그를 남기고, 모든 DB 데이터를 읽어서 메모리에서 페이징을 시도한다.
    - 최악의 경우 장애로 이어질 수 있다.
### 한계 돌파
그럼 페이징 + 컬렉션 엔티티를 함께 조회하려면 어떻게 해야 할까?<br>
지금부터 코드도 단순하고, 성능 최적화도 보장하는 매우 강력한 방법을 소개한다.<br>
대부분의 `페이징 + 컬렉션 엔티티 조회` 문제는 이 방법으로 해결할 수 있다.
- 먼저 `ToOne`(OneToOne, ManyToOne) 관계를 모두 `페치 조인`한다.
    - ToOne 관계는 row 수를 증가시키지 않으므로, 페이징 쿼리에 영향을 주지 않는다.
- `컬렉션`은 `지연 로딩`으로 조회한다.
- 지연 로딩 성능 최적화를 위해 `hibernate.default_batch_fetch_size`, `@BatchSize`를 적용한다.
    - hibernate.default_batch_fetch_size: 글로벌 설정
    - @BatchSize: 개별 최적화
    - 이 옵션을 사용하면 컬렉션이나, 프록시 객체를 한번에 설정한 size 만큼 `IN 쿼리`로 조회한다.
### OrderRepository에 추가
```java
public List<Order> findAllWithMemberDelivery(int offset, int limit) {
    return em.createQuery(
            "select o from Order o " +
                    "join fetch o.member m " +
                    "join fetch o.delivery d", Order.class)
            .setFirstResult(offset)
            .setMaxResults(limit)
            .getResultList();
}
```
### OrderApiController에 추가
```java
@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;

    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> ordersV3_page(
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "100") int limit) {
        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit);

        return orders.stream()
                .map(OrderDto::new)
                .collect(Collectors.toList());
    }

    @Getter
    static class OrderDto {

        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems;

        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getOrderStatus();
            address = order.getDelivery().getAddress();
            orderItems = order.getOrderItems().stream()
                    .map(orderItem -> new OrderItemDto(orderItem))
                    .collect(Collectors.toList());
        }
    }

    @Getter
    static class OrderItemDto {

        private String itemName;
        private int orderPrice;
        private int count;

        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();;
        }
    }
}
```
### 최적화 옵션
```properties
spring.jpa.properties.hibernate.default_batch_fetch_size=1000
```
- 개별로 설정하려면, `@BatchSize`를 적용하면 된다. (컬렉션은 컬렉션 필드에, 엔티티는 엔티티 클래스에 적용)
- <b>장점</b>
    - 쿼리 호출 수가 `1 + N` -> `1 + 1`로 최적화 된다.
    - 조인보다 DB 데이터 전송량이 최적화 된다.
        - Order와 OrderItem을 조인하면, Order가 OrderItem만큼 중복해서 조회된다.
        - 이 방법은 각각 조회하므로 전송해야 할 중복 데이터가 없다.
    - 페치 조인 방식과 비교해서 쿼리 호출 수가 약간 증가하지만, DB 데이터 전송량이 감소한다.
    - 컬렉션 페치 조인은 페이징이 불가능 하지만, 이 방법은 페이징이 가능하다.
- <b>결론</b>
    - ToOne 관계는 페치 조인해도 페이징에 영향을 주지 않는다.
    - 따라서 ToOne 관계는 페치 조인으로 쿼리 수를 줄여 해결하고, 나머지는 `hibernate.default_batch_fetch_size`로 최적화하자.
> <b>참고</b>
> - `default_batch_fetch_size`의 크기는 적당한 사이즈를 골라야 하는데, 100 ~ 1,000 사이를 선택하는 것을 권장한다.
> - 이 전략은 SQL IN 절을 사용하는데, 데이터베이스에 따라 IN절 파라미터를 1,000으로 제한하기도 한다.
> - 1,000으로 잡으면 한 번에 1,000개를 DB에서 애플리케이션에 불러오므로 DB에 순간 부하가 증가할 수 있다.
> - 하지만 애플리케이션은 100이든 1,000이든 결국 전체 데이터를 로딩해야 하므로 메모리 사용량은 같다.
> - 1,000으로 설정하는 것이 성능상 가장 좋지만, 결국 DB든 애플리케이션이든 순간 부하를 어디까지 견딜 수 있는지로 결정하면 된다.
___
## 주문 조회 V4: JPA에서 DTO 직접 조회
```java
@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;

    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> ordersV4() {
        return orderQueryRepository.findOrderQueryDtos();
    }

    @Getter
    static class OrderDto {

        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems;

        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getOrderStatus();
            address = order.getDelivery().getAddress();
            orderItems = order.getOrderItems().stream()
                    .map(orderItem -> new OrderItemDto(orderItem))
                    .collect(Collectors.toList());
        }
    }

    @Getter
    static class OrderItemDto {

        private String itemName;
        private int orderPrice;
        private int count;

        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();;
        }
    }
}
```
### OrderQueryRepository 추가
```java
@Repository
@RequiredArgsConstructor
public class OrderQueryRepository {

    private final EntityManager em;

    public List<OrderQueryDto> findOrderQueryDtos() {
        List<OrderQueryDto> orders = findOrders();

        orders.forEach(o -> {
            List<OrderItemQueryDto> orderItems = findOrderItems(o.getOrderId());
            o.setOrderItems(orderItems);
        });

        return orders;
    }

    private List<OrderItemQueryDto> findOrderItems(Long orderId) {
        return em.createQuery(
                "select new jpabook.jpashop.repository.order.query.OrderItemQueryDto(oi.order.id, i.name, oi.orderPrice, oi.count) " +
                        "from OrderItem oi " +
                        "join oi.item i " +
                        "where oi.order.id = :orderId", OrderItemQueryDto.class)
                .setParameter("orderId", orderId)
                .getResultList();
    }

    private List<OrderQueryDto> findOrders() {
        return em.createQuery(
                "select new jpabook.jpashop.repository.order.query.OrderQueryDto(o.id, m.name, o.orderDate, o.orderStatus, d.address) " +
                        "from Order o " +
                        "join o.member m " +
                        "join o.delivery d", OrderQueryDto.class
        ).getResultList();
    }

}
```
### OrderQueryDto 추가
```java
@Getter
@EqualsAndHashCode(of = "orderId")
@AllArgsConstructor
public class OrderQueryDto {

    private Long orderId;
    private String name;
    private LocalDateTime orderDate;
    private OrderStatus orderStatus;
    private Address address;
    private List<OrderItemQueryDto> orderItems = new ArrayList<>();

    public OrderQueryDto(Long orderId, String name, LocalDateTime orderDate, OrderStatus orderStatus, Address address) {
        this.orderId = orderId;
        this.name = name;
        this.orderDate = orderDate;
        this.orderStatus = orderStatus;
        this.address = address;
    }

    public void setOrderItems(List<OrderItemQueryDto> orderItems) {
        this.orderItems = orderItems;
    }
}
```
### OrderItemQueryDto 추가
```java
@Getter
@AllArgsConstructor
public class OrderItemQueryDto {

    @JsonIgnore
    private Long orderId;
    private String itemName;
    private int orderPrice;
    private int count;
}
```
- Query: 루트 1번, 컬렉션 N번 실행
- ToOne(N:1, 1:1) 관계들을 먼저 조회하고, ToMany(1:N) 관계는 각각 별도로 처리한다.
    - 이런 방식을 선택한 이유는 다음과 같다.
        - ToOne 관계는 조인해도 데이터 row 수가 증가하지 않는다.
        - ToMany(1:N) 관계는 조인하면 row 수가 증가한다.
- row 수가 증가하지 않는 ToOne 관계는 조인으로 최적화 하기 쉬우므로, 한 번에 조회하고, ToMany 관계는 최적화 하기 어려우므로 `findOrderItems()` 와 같은 별도의 메소드로 조회한다.
___
## 주문 조회 V5: JPA에서 DTO 직접 조회 - 컬렉션 조회 최적화
### OrderApiController에 추가
```java
@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;

    @GetMapping("/api/v5/orders")
    public List<OrderQueryDto> ordersV5() {
        return orderQueryRepository.findAllByDto_optimization();
    }

    @Getter
    static class OrderDto {

        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems;

        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getOrderStatus();
            address = order.getDelivery().getAddress();
            orderItems = order.getOrderItems().stream()
                    .map(orderItem -> new OrderItemDto(orderItem))
                    .collect(Collectors.toList());
        }
    }

    @Getter
    static class OrderItemDto {

        private String itemName;
        private int orderPrice;
        private int count;

        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();;
        }
    }
}
```
### OrderQueryRepository에 추가
```java
@Repository
@RequiredArgsConstructor
public class OrderQueryRepository {

    private final EntityManager em;

    public List<OrderQueryDto> findAllByDto_optimization() {
        List<OrderQueryDto> orders = findOrders();

        Map<Long, List<OrderItemQueryDto>> orderItemMap = findOrderItemMap(toOrderIds(orders));

        orders.forEach(o -> o.setOrderItems(orderItemMap.get(o.getOrderId())));

        return orders;
    }

    private Map<Long, List<OrderItemQueryDto>> findOrderItemMap(List<Long> orderIds) {
        List<OrderItemQueryDto> orderItems = em.createQuery(
                "select new jpabook.jpashop.repository.order.query.OrderItemQueryDto(oi.order.id, i.name, oi.orderPrice, oi.count) " +
                        "from OrderItem oi " +
                        "join oi.item i " +
                        "where oi.order.id in :orderIds", OrderItemQueryDto.class)
                .setParameter("orderIds", orderIds)
                .getResultList();

        Map<Long, List<OrderItemQueryDto>> orderItemMap = orderItems.stream()
                .collect(Collectors.groupingBy(OrderItemQueryDto::getOrderId));
        return orderItemMap;
    }

    private List<Long> toOrderIds(List<OrderQueryDto> orders) {
        return orders.stream()
                .map(OrderQueryDto::getOrderId)
                .collect(Collectors.toList());
    }
}
```
- Query: 루트 1번, 컬렉션 1번
- ToOne 관계들을 먼저 조회하고, 여기서 얻은 식별자 orderId로 ToMany 관계인 `OrerItem`을 한꺼번에 조회
- Map을 사용해서 매칭 성능 향상(O(1))
___
## 주문 조회 V6: JPA에서 DTO로 직접 조회 - 플랫 데이터 최적화
### OrderApiController에 추가
```java
@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;

    @GetMapping("/api/v6/orders")
    public List<OrderQueryDto> ordersV6() {
        List<OrderFlatDto> flats = orderQueryRepository.findAllByDto_flat();

        return flats.stream()
                .collect(groupingBy(o -> new OrderQueryDto(o.getOrderId(), o.getName(), o.getOrderDate(), o.getOrderStatus(), o.getAddress()),
                        mapping(o -> new OrderItemQueryDto(o.getOrderId(), o.getItemName(), o.getOrderPrice(), o.getCount()), toList())
                )).entrySet().stream()
                .map(e -> new OrderQueryDto(e.getKey().getOrderId(), e.getKey().getName(), e.getKey().getOrderDate(), e.getKey().getOrderStatus(), e.getKey().getAddress(), e.getValue()))
                .collect(toList());
    }

    @Getter
    static class OrderDto {

        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems;

        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getOrderStatus();
            address = order.getDelivery().getAddress();
            orderItems = order.getOrderItems().stream()
                    .map(orderItem -> new OrderItemDto(orderItem))
                    .collect(Collectors.toList());
        }
    }

    @Getter
    static class OrderItemDto {

        private String itemName;
        private int orderPrice;
        private int count;

        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();;
        }
    }
}
```
### OrderQueryRepository
```java
@Repository
@RequiredArgsConstructor
public class OrderQueryRepository {

    private final EntityManager em;

    public List<OrderFlatDto> findAllByDto_flat() {
        return em.createQuery(
                "select new jpabook.jpashop.repository.order.query.OrderFlatDto(o.id, m.name, o.orderDate, o.orderStatus, d.address, i.name, oi.orderPrice, oi.count) " +
                        "from Order o " +
                        "join o.member m " +
                        "join o.delivery d " +
                        "join o.orderItems oi " +
                        "join oi.item i", OrderFlatDto.class)
                .getResultList();
    }
}
```
### OrderFlatDto 추가
```java
@Getter
@AllArgsConstructor
public class OrderFlatDto {

    private Long orderId;
    private String name;
    private LocalDateTime orderDate;
    private OrderStatus orderStatus;
    private Address address;

    private String itemName;
    private int orderPrice;
    private int count;
}
```
- Query: 1번
- <b>단점</b>
    - 쿼리는 한 번이지만, 조인으로 인해 DB에서 애플리케이션에 전달하는 데이터에 중복 데이터가 추가되므로, 상황에 따라 V5보다 더 느릴 수도 있다.
    - 애플리케이션에서 추가 작업이 크다.
    - 페이징❌
___
## 정리
- 엔티티 조회 
    - 엔티티를 조회해서 그대로 반환: V1
    - 엔티티 조회 후 DTO로 변환: V2
    - 페치 조인으로 쿼리 수 최적회: V3
    - 컬렉션 페이징과 한계 돌파: V3.1
        - 컬렉션은 페치 조인 시, 페이징 불가
        - ToOne 관계는 페치 조인으로 쿼리 수 최적화
        - 컬렉션은 페치 조인 대신 지연 로딩을 유지하고, `hibernate.default_batch_fetch_size`, `@BatchSize`로 최적화
- DTO 직접 조회
    - JPA에서 DTO를 직접 조회: V4
    - 컬렉션 조회 최적화: V5
        - 일대다 관계인 컬렉션은 IN 절을 활용하여 메모리에 미리 조회해서 최적화
    - 플랫 데이터 최적화: V6
        - JOIN 결과를 그대로 조회 후, 애플리케이션에서 원하는 모양으로 직졉 변환
### 권장 순서
1. 엔티티 조회 방식으로 우선 접근
    1. 페치 조인으로 쿼리 수를 최적화
    2. 컬렉션 최적화
        1. 페이징 필요 `hibernate.default_batch_fetch_size`, `@BatchSize`로 최적화
        2. 페이징 필요❌ -> 페치 조인 사용
2. 엔티티 조회 방식으로 해결이 안되면, DTO 조회 방식 사용
3. DTO 조회 방식으로 해결이 안되면, NativeSQL or 스프링 JdbcTemplate 사용
> <b>참고</b>
> - 엔티티 조회 방식은 페치 조인이나, `hibernate.default_batch_fetch_size`, `@BatchSize`같이 코드를 거의 수정하지 않고, 옵션만 약간 변경해서 다양한 성능 최적화를 시도할 수 있다.
> - 반면, DTO를 직접 조회하는 방식은 성능을 최적화하거나, 성능 최적화 방식을 변경할 때 많은 코드를 변경해야 한다.

> <b>참고</b>
> - 개발자는 성능 최적화와 코드 복잡도 사이에서 줄타기를 해야 한다.
>   - 항상 그런 것은 아니지만, 보통 성능 최적화는 단순한 코드를 복잡한 코드로 몰고 간다.
> - 엔티티 조회 방식은 JPA가 많은 부분을 최적화 해주기 때문에, 단순한 코드를 유지하면서 성능을 최적화 할 수 있다.
> - 반면 DTO 조회 방식은 SQL을 직접 다루는 것과 유사하기 때문에, 둘 사이에서 줄타기를 해야 한다.

### DTO 조회 방식의 선택지
- DTO로 조회하는 방법도 각각 장단점이 있다.
    - V4, V5, V6 에서 단순히 쿼리가 1번만 실행된다고 V6이 항상 좋은 방법인 것은 아니다.
- V4는 코드가 단순하며, 특정 주문 한 건만 조회하면, 이 방식을 사용해도 성능이 잘 나온다.
    - 예를 들어 조회한 Order 데이터가 1건이면 ORderItem을 찾기 위한 쿼리도 1번만 실행하면 된다.
- V5는 코드가 복잡하며, 여러 주문을 한꺼번에 조회하는 경우에는 V4 대신 V5 방식을 사용해야 한다. 
    - 예를 들어 조회한 Order 데이터가 1,000건인데, V4 방식을 사용하면, 쿼리가 총 1 + 1,000 번 실행된다.
    - 여기서 1은 Order를 조회한 쿼리고, 1,000은 조회된 Order의 row 수다.
    - V5 방식으로 최적화하면 쿼리가 총 1 + 1 번만 실행된다.
    - 상황에 따라 다르겠지만 운영 환경에서 100배 이상의 성능 차이가 날 수 있다.
- V6은 완전히 다른 접근 방식이며, 쿼리 한 번으로 최적화 되어 상당히 좋아 보이지만, Order를 기준으로 페이징이 불가능하다.
    - 실무에서 이정도 데이터면 수 백, 수 천건 단위로 페이징 처리가 꼭 필요하므로, 이 경우 선택하기 어려운 방법이다.
    - 그리고 데이터가 많으면 중복 전송이 증가해서 V5와 비교해서 성능 차이도 미비하다.