package hello.servlet.web.frontcontroller.v4.controller;

import hello.servlet.domain.member.Member;
import hello.servlet.domain.member.MemberRepository;
import hello.servlet.web.frontcontroller.v4.ControllerV4;

import java.util.List;
import java.util.Map;

public class MemberListControllerV4 implements ControllerV4 {

    private final MemberRepository memberRepository = MemberRepository.getInstance();

    @Override
    public String process(Map<String, String> paramMap, Map<String, Object> model) {
        List<Member> members = memberRepository.findAll();

        // Call-By-Value(Java)
        // 레퍼런스 변수를 값으로 전달받은 새로운 지역 변수 = model
        // model 변수의 주소에 해당 주소값을 복사하여 저장
        // put 메소드 사용 -> 동일한 주소값의 상태를 변경
        // 결과적으로, FrontController의 model에도 ("members", members)이 저장된다.
        model.put("members", members);
        return "members";
    }
}
