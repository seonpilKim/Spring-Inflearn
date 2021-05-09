package hello.core.singleton;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class StatefulServiceTest {

    @Test
    void statefulServiceSingleton(){
        ApplicationContext ac = new AnnotationConfigApplicationContext(TestConfig.class);
        StatefulService statefulService = ac.getBean(StatefulService.class);
        StatefulService statefulService1 = ac.getBean(StatefulService.class);

        // ThreadA: A사용자 10000원 주문
        int userAPrice = statefulService.order("userA", 10000);
        // ThreadA: B사용자 20000원 주문
        int userBPrice = statefulService1.order("userB", 20000);

        // ThreadA: 사용자A 주문 금액 조회
//        int price = statefulService.getPrice();
        // ThreadA: 사용자A는 10000원을 기대했지만, 기대와 다르게 20000원 출력
//        System.out.println("price = " + price);

//        assertThat(statefulService.getPrice()).isEqualTo(20000);

        System.out.println("price = " + userAPrice);

    }

    static class TestConfig{

        @Bean
        public StatefulService statefulService(){
            return new StatefulService();
        }
    }

}