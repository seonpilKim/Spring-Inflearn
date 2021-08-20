package jpabook.jpashop.service;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

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