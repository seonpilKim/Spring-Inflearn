# 회원 관리 예제 - 웹 MVC 개발
- 회원 웹 기능 - 홈 화면 추가
- 회원 웹 기능 - 등록
- 회원 웹 기능 - 조회

## 회원 웹 기능 - 홈 화면 추가

### 홈 컨트롤러 추가
```java
package hello.hellospring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(){
        return "home";
    }
}
```
- `/`은 localhost:8080의 홈을 의미하며, 해당 서버에 접속하면, template 폴더에서 `home` 이름을 가진 파일을 찾아 반환한다.
- static 폴더에 `index.html`가 존재하지만, 컨트롤러가 정적 파일보다 우선순위가 높기 때문에, template 폴더의 `home.html`이 반환되는 것이다.
- 순서
    1. `localhost:8080/` 접속
    2. 스프링 컨테이너에 `/` 관련 컨트롤러가 존재하는지 확인
        - 존재한다면, 해당 컨트롤러에서 template 반환
        - 존재하지 않는다면, static 폴더 or template 폴더에 존재하는 index.html 파일 반환

### 회원 관리용 홈
```html
<!DOCTYPE HTML>
<html xmlns:th="http://www.thymeleaf.org"/>
<body>
<div class="container">
    <div>
        <h1>Hello Spring</h1>
        <p>회원 기능</p>
        <p>
            <a href="/members/new">회원 가입</a>
            <a href="/members">회원 목록</a>
        </p>
    </div>
</div>
</body>
```

## 회원 웹 기능 - 등록

### 회원 등록 폼 개발
- `회원 등록 폼 컨트롤러`
```java
@Controller
public class MemberController {
    private final MemberService memberService;

    @Autowired
    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/members/new")
    public String createForm(){
        return "members/createMemberform";
    }
}
```
- `회원 등록 폼 HTML`(resources/templates/members/createMemberform)
```html
<!DOCTYPE HTML>
<html xmlns:th="http://www.thymeleaf.org">
<body>
<div class="container">
    <form action="/members/new" method="post">
        <div class="form-group">
            <label for="name">이름</label>
            <input type="text" id="name" name="name" placeholder="이름을 입력하세요"/>
        </div>
        <button type="submit">등록</button>
    </form>
</div>
</body>
</html>
```
- `name=""` 안의 값이 `key` 역할을 하며, `input 값`이 `value` 역할을 한다.

### 회원 등록 컨트롤러
- `웹 등록 화면에서 데이터를 전달 받을 폼 객체`
```java
package hello.hellospring.controller;

public class MemberForm {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
```
- `자바빈 프로퍼티 규약` : 회원 등록 폼 HTML에서 name="" 안의 값(key)과 getter/setter 메소드의 이름을 맞추어야 한다.
- `스프링 MVC`은 기본적으로 웹의 parameter에 name이라는 이름이 있으면, 이 이름을 보고 setName을 호출하는 기능을 지원한다.

- `회원 컨트롤러에서 회원을 실제 등록하는 기능`
```java
@Controller
public class MemberController {
    ...
    
    @PostMapping("/members/new")
    public String create(MemberForm form){
        Member member = new Member();
        member.setName(form.getName());

        memberService.join(member);

        return "redirect:/";
    }
}
```
- 참고
    - `get` 방식 : 데이터 조회(url 입력)
    - `post` 방식 : 데이터 전달

## 회원 웹 기능 - 조회
### 회원 컨트롤러에서 조회 기능
```java
@Controller
public class MemberController {
    ...

    @GetMapping("/members")
    public String list(Model model) {
        List<Member> members = memberService.findMembers();
        model.addAttribute("members", members);
        return "members/memberList";
    }
}
```

### 회원 리스트 HTML
```html
<!DOCTYPE HTML>
<html xmlns:th="http://www.thymeleaf.org">
<body>
<div class="container">
    <div>
        <table>
            <thead>
            <tr>
                <th>#</th>
                <th>이름</th>
            </tr>
            </thead>
            <tbody>
            <!-- th: 는 타임리프 문법 -->
            <tr th:each="member : ${members}">
                <!-- id, name은 getter 메소드를 이용하여 데이터를 가져온다 -->
                <td th:text="${member.id}"></td>
                <td th:text="${member.name}"></td>
            </tr>
            </tbody>
        </table>
    </div>
</div>
</body>
</html>
```