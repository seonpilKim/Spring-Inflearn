package hello.core;

import hello.core.discount.DiscountPolicy;
import hello.core.discount.RateDiscountPolicy;
import hello.core.member.MemberRepository;
import hello.core.member.MemberService;
import hello.core.member.MemberServiceImpl;
import hello.core.member.MemoryMemberRepository;
import hello.core.order.OrderService;
import hello.core.order.OrderServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 객체를 생성하고 관리하면서 의존관계를 연결해주므로
// AppConfig를 DI 컨테이너 라고 부른다.
@Configuration
public class AppConfig {

    // 외부(AppConfig)에서 프로그램 제어 흐름을 제어하므로 제어의 역전(IoC)에 해당된다.
    @Bean
    public MemberService memberService(){
        // 생성자 주입
        return new MemberServiceImpl(memberRepository());
    }

    @Bean
    public OrderService orderService(){
        // 생성자 주입
        return new OrderServiceImpl(memberRepository(), discountPolicy());
    }

    // 리팩터링
    // 구현체 변경 시, 이 부분만 변경하면 됨.
    @Bean
    public MemberRepository memberRepository() {
        return new MemoryMemberRepository();
    }

    // 리팩터링
    // 구현체 변경 시, 이 부분만 변경하면 됨.
    @Bean
    public DiscountPolicy discountPolicy() {
        return new RateDiscountPolicy();
    }
}
