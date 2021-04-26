package hello.hellospring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HelloController {

    // http의 get method.
    // hello 라는 url에 매칭되어, http://localhost:8080/hello 에 접속하면 아래의 method가 실행이 된다.
    @GetMapping("hello")
    public String hello(@RequestParam(name="name", required=false, defaultValue="World") String name, Model model){
        model.addAttribute("name", name);
        /*
            기본적으로, spring boot는 아래와 같이 세팅되어있다.
             resources/templates 폴더에있는 hello를 찾아서 랜더링한다.
        */
        return "hello";
    }
}


