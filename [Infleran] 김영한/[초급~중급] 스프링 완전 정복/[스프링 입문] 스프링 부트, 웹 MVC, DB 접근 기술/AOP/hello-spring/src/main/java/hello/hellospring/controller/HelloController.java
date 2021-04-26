package hello.hellospring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

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

    // required default 값은 true이다.
    // 그렇기 때문에, url 뒤에 ?name=spring 처럼 추가해주어야 오류가 나지 않음!
    // 추가로, @Requestparam을 사용하지 않으면, 시스템에 따라 자바 컴파일러 최적화 옵션에 의해 컴파일 시점에 name 이라는 변수가 사라지기 때문에, 받을 수가 없게 된다.
    // @RequestParam("name") String name -> 컴파일 후 @RequestParam("name") String x01
    // 그러므로 @Requestparam을 사용하는 것이 좋다.
    @GetMapping("hello-mvc")
    public String helloMvc(@RequestParam("name") String name, Model model){
        model.addAttribute("name", name);
        return "hello-template";
    }

    @GetMapping("hello-string")
    @ResponseBody   // HTTP의 Body 부분에 아래 내용을 그대로 넣겠다는 의미.
    public String helloString(@RequestParam("name") String name){
        return "hello " + name;
        // "hello spring"
    }

    @GetMapping("hello-api")
    @ResponseBody   // json으로 반환하는 것이 default로 설정되어있다.
    /*
        @ResponseBody가 없을 때는, return 값을 viewResolver에게 전달하여 맞는 template을 찾아서 반환한다.
        @ResponseBody가 있을 때는, HTTP 응답에 그대로 데이터를 넘긴다.
        만약, 문자가 아니라 객체를 반환하는 경우 json 방식(default)으로 데이터를 구성해서 HTTP 응답에 반환한다.
        HttpMessageConverter가 위의 과정을 처리한다.
    */
    public Hello helloApi(@RequestParam("name") String name){
        // Ctrl + Shift + Enter : 문법적으로 오류가 있는 부분 자동으로 수정.
        Hello hello = new Hello();
        hello.setName(name);
        return hello;
        // {"name" : "spring"} -> json 구조 (key, value)
    }

    static class Hello{
        private String name;
        // Alt + Insert 단축키로 getter, setter 함수를 자동으로 완성시킬 수 있다.
        // Java Beans 표준 방식
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}


