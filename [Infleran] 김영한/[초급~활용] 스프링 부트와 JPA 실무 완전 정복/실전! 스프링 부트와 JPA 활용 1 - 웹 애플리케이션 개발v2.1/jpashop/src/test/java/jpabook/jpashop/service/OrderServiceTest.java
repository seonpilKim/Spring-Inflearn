package jpabook.jpashop.service;

import jpabook.jpashop.domain.Item.Book;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.exception.NotEnoughStockException;
import jpabook.jpashop.repository.OrderRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class OrderServiceTest {

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OrderService orderService;

    @Autowired
    EntityManager em;

    private Book createBook(String name, int price, int stockQuantity, String author, String isbn) {
        Book book = new Book(name, price, stockQuantity, author, isbn);
        em.persist(book);
        return book;
    }

    private Member createMember(String name, String city, String street, String zipcode) {
        Member member = new Member(name);
        member.updateAddress(city, street, zipcode);
        em.persist(member);
        return member;
    }

    @Test
    void 상품주문() throws Exception {
        // given
        Member member = createMember("memberA", "서울", "강가", "123-123");
        Book book = createBook("SpringBoot", 30000, 100, "김영한", "12452113");

        int orderCount = 2;

        // when
        Long orderId = orderService.order(member.getId(), book.getId(), orderCount);

        // then
        Order findOrder = orderRepository.findOne(orderId);

        assertEquals(findOrder.getOrderStatus(), OrderStatus.ORDER, "상품 주문 시, 상태는 ORDER");
        assertEquals(findOrder.getOrderItems().size(), 1, "주문한 상품 종류 수는 1");
        assertEquals(findOrder.getTotalPrice(), book.getPrice() * orderCount, "주문 가격 = 가격 * 수량");
        assertEquals(book.getStockQuantity(), 100 - orderCount, "주문 수량만큼 재고가 감소해야 한다.");
    }

    @Test
    void 주문취소() throws Exception {
        // given
        Member member = createMember("memberA", "보성", "복내월평길", "25-6");
        Book book = createBook("JPA", 25000, 100, "김영한", "182456");

        int orderCount = 50;

        Long orderId = orderService.order(member.getId(), book.getId(), orderCount);

        // when
        orderService.cancelOrder(orderId);

        // then
        assertEquals(OrderStatus.CANCEL, orderRepository.findOne(orderId).getOrderStatus(),
                "주문을 취소하면 상태가 CANCEL 으로 바뀌어야 한다.");
        assertEquals(100, book.getStockQuantity(),
                "주문을 취소한 만큼 재고 수량이 증가해야 한다.");
    }
    
    @Test
    void 상품주문_재고수량초과() throws Exception {
        // given
        Member member = createMember("memberA", "보성", "복내월평길", "25-6");
        Book book = createBook("JPA", 25000, 100, "김영한", "182456");

        int orderCount = 101;

        // when
        NotEnoughStockException ex = assertThrows(NotEnoughStockException.class, () -> orderService.order(member.getId(), book.getId(), orderCount));

        // then
        assertEquals("need more stock", ex.getMessage());
    }
}