# 빈 생명주기 콜백
## 목차
- 빈 생명주기 콜백 시작
- 인터페이스 InitializaingBean, DisposableBean
- 빈 등록 초기화, 소멸 메소드 지정
- 애노테이션 @PostConstrict, @PreDestroy
___
## 빈 생명주기 콜백 시작
- 데이터베이스 커넥션 풀이나, 네트워크 소켓처럼 애플리케이션 시작 시점에 필요한 연결을 미리 해두고, 애플리케이션 종료 시점에 연결을 모두 종료하는 작업을 진행하려면, 객체의 초기화와 종료 작업이 필요하다.
- 스프링을 통해 이러한 초기화 작업과 종료 작업을 어떻게 진행하는지 예제로 알아보자.<br><br>
- 간단하게 외부 네트워크에 미리 연결하는 객체를 하나 생성한다고 가정해보자.
    - 실제로 네트워크에 연결하진 않고, 단순히 문자만 출력하도록 했다.
    - 이 `NetworkClient`는 애플리케이션 시작 시점에 `connect()`를 호출해서 연결을 맺어두어야 하고, 애플리케이션이 종료되면 `disconnect()`를 호출해서 연결을 끊어야 한다.
- <b>예제 코드, 테스트 하위에 생성</b>
```java
public class NetworkClient {
    private String url; 

    public NetworkClient() {
        System.out.println("생성자 호출, url = " + url);
        connect();
        call("초기화 연결 메시지");
    }

    public void setUrl(String url) {
        this.url = url;
    }

    //서비스 시작시 호출
    public void connect() {
        System.out.println("connect: " + url);
    }

    public void call(String message) {
        System.out.println("call: " + url + " message = " + message);
    }

    //서비스 종료시 호출
    public void disconnect() {
        System.out.println("close: " + url);
    }
}
```
- <b>스프링 환경설정과 실행</b>
```java
public class BeanLifeCycleTest {
    @Test
    public void lifeCycleTest() {
        ConfigurableApplicationContext ac = new AnnotationConfigApplicationContext(LifeCycleConfig.class);
        NetworkClient client = ac.getBean(NetworkClient.class);
        ac.close(); //스프링 컨테이너를 종료, ConfigurableApplicationContext 필요
    }

    @Configuration
    static class LifeCycleConfig {
        @Bean
        public NetworkClient networkClient() {
            NetworkClient networkClient = new NetworkClient();
            networkClient.setUrl("http://hello-spring.dev");
            return networkClient;
        }
    }
}
```
- 실행해보면 다음과 같은 의도하지 않은 결과가 나온다.
```
생성자 호출, url = null
connect: null
call: null message = 초기화 연결 메시지
```
- 생성자 부분을 보면, url정보 없이 connect가 호출되는 것을 확인할 수 있다.
- 너무 당연한 이야기지만, 객체를 생성하는 단계에는 url이 없고, 객체를 생성한 다음 외부에서 수정자 주입을 통해 `setUrl()`이 호출되어야 url이 존재하게 된다.<br><br>
- 스프링 빈은 간단하게 다음과 같은 라이프사이클을 가진다.
    - <b>객체 생성 -> 의존관계 주입</b>
- 스프링 빈은 객체를 생성하고, 의존관계 주입이 다 끝난 다음에야 필요한 데이터를 사용할 수 있는 준비가 완료된다.
    - 따라서 초기화 작업은 의존관계 주입이 모두 완료되고 난 다음에 호출해야 한다.
    - 그런데 개발자가 의존관계 주입이 모두 완료된 시점을 어떻게 알 수 있을까?
- <b>스프링은 의존관계 주입이 완료되면, 스프링 빈에게 콜백 메소드를 통해 초기화 시점을 알려주는 다양한 기능을 제공</b>한다.
- 또한, <b>스프링은 스프링 컨테이너가 종료되기 직전에 소멸 콜백</b>을 준다.
    - 따라서 안전하게 종료 작업을 진행할 수 있다.
- <b>스프링 빈의 이벤트 라이프사이클</b>
    - <b>스프링 컨테이너 생성 -> 스프링 빈 생성 -> 의존관계 주입 -> 초기화 콜백 -> 사용 -> 소멸전 콜벡 -> 스프링 종료</b> 
- <b>초기화 콜백:</b> 빈이 생성되고, 빈의 의존관계 주입이 완료된 후 호출
- <b>소멸전 콜백:</b> 빈이 소멸되기 직전에 호출<br><br>
- 스프링은 다양한 방식으로 생명주기 콜백을 지원한다.
> <b>참고: 객체의 생성과 초기화를 분리하자.</b>
> - 생성자는 필수 정보(파라미터)를 받고, 메모리를 할당해서 객체를 생성하는 책임을 가진다.
> - 반면 초기화는 이렇게 생성된 값들을 활용하여 외부 커넥션을 연결하는 등의 무거운 동작을 수행한다.
> - 따라서 생성자 안에서 무거운 초기화 작업을 함께 하는 것 보다는, 객체를 생성하는 부분과 초기화를 하는 부분을 명확하게 나누는 것이 유지보수 관점에서 좋다.
> - 물론 초기화 작업이 내부 값들만 약간 변경하는 정도로 단순한 경우에는 생성자에서 한 번에 다 처리하는 게 더 나을 수도 있다.

> <b>참고</b>
> - 싱글톤 빈들은 스프링 컨테이너가 종료될 때, 싱글톤 빈들도 함께 종료되기 때문에 스프링 컨테이너가 종료되기 직전에 소멸전 콜백이 일어난다.
> - 싱글톤처럼 컨테이너의 시작과 종료까지 생존하는 빈도 있지만, 생명주기가 짧은 빈들도 있는데, 이 빈들은 컨테이너와 무관하게 해당 빈이 종료되기 직전에 소멸전 콜벽이 일어난다.
>   - 자세한 내용은 스코프에서 알아본다.
- <b>스프링은 크게 3가지 방법으로 빈 생명주기 콜백을 지원한다.</b>
    - 인터페이스(InitializingBean, DisposableBean)
    - 설정 정보에 초기화 메소드, 종료 메소드 지정
    - @PostConsturct, @PreDestory 애노테이션 지원
### 인터페이스 InitializingBean, DisposableBean
```java
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class NetworkClient implements InitializingBean, DisposableBean {
    private String url;

    public NetworkClient() {
        System.out.println("생성자 호출, url = " + url);
    }

    public void setUrl(String url) {
        this.url = url;
    }

    //서비스 시작시 호출
    public void connect() {
        System.out.println("connect: " + url);
    }

    public void call(String message) {
        System.out.println("call: " + url + " message = " + message);
    }

    //서비스 종료시 호출
    public void disConnect() {
        System.out.println("close + " + url);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        connect(); 
        call("초기화 연결 메시지");
    }

    @Override
    public void destroy() throws Exception {
        disConnect();
    }
}
```
- `InitializingBean`은 `afterPropertiesSet()` 메소드로 초기화를 지원한다.
- `DisposableBean`은 `destory()` 메소드로 소멸을 지원한다.<br><br>
- <br>출력 결과</br>
```
생성자 호출, url = null
NetworkClient.afterPropertiesSet
connect: http://hello-spring.dev
call: http://hello-spring.dev message = 초기화 연결 메시지
13:24:49.043 [main] DEBUG 
org.springframework.context.annotation.AnnotationConfigApplicationContext - Closing NetworkClient.destroy
close + http://hello-spring.dev
```
- 출력 결과를 보면, 초기화 메소드가 주입 완료 후에 적절하게 호출된 것을 확인할 수 있다.
- 그리고 스프링 컨테이너의 종료가 호출되자 소멸 메소드가 호출된 것도 확인할 수 있다.<br><br>
- <b>초기화, 소멸 인터페이스 단점</b>
    - 이 인터페이스는 스프링 전용 인터페이스다, 해당 코드가 스프링 전용 인터페이스에 의존한다.
    - 초기화, 소멸 메소드의 이름을 변경할 수 없다.
    - 내가 코드를 고칠 수 없는 외부 라이브러리에 적용할 수 없다.
> <b>참고</b>: 인터페이스를 사용하는 초기화, 종료 방법은 스프링 초창기에 나온 방법들이고, 지금은 다음의 더 나은 방법들이 있어서 거의 사용하지 않는다.
### 빈 등록 초기화, 소멸 메소드 지정
- 설정 정보에 `@Bean(initMethod = "init", destoryMethod = "close")`처럼 초기화, 소멸 메소드를 지정할 수 있다.<br><br>
- <b>설정 정보를 사용하도록 변경</b>
```java
public class NetworkClient {
    private String url;

    public NetworkClient() {
        System.out.println("생성자 호출, url = " + url);
    }

    public void setUrl(String url) {
        this.url = url;
    }

    //서비스 시작시 호출
    public void connect() {
        System.out.println("connect: " + url);
    }

    public void call(String message) {
        System.out.println("call: " + url + " message = " + message);
    }

    //서비스 종료시 호출
    public void disConnect() {
        System.out.println("close + " + url);
    }

    @Override
    public void init() {
        connect(); 
        call("초기화 연결 메시지");
    }

    @Override
    public void close() {
        disConnect();
    }
}
```
- <b>설정 정보에 초기화 소멸 메소드 지정</b>
```java
@Configuration
static class LifeCycleConfig {
    @Bean(initMethod = "init", destroyMethod = "close")
    public NetworkClient networkClient() {
        NetworkClient networkClient = new NetworkClient();
        networkClient.setUrl("http://hello-spring.dev");
        return networkClient;
    }
}
```
- <b>출력 결과</b>
```
생성자 호출, url = null
NetworkClient.init
connect: http://hello-spring.dev
call: http://hello-spring.dev message = 초기화 연결 메시지
13:33:10.029 [main] DEBUG 
org.springframework.context.annotation.AnnotationConfigApplicationContext - Closing NetworkClient.close
close + http://hello-spring.dev
```
- <b>설정 정보 사용 특징</b>
    - 메소드 이름을 자유롭게 줄 수 있다.
    - 스프링 빈이 스프링 코드에 의존하지 않는다.
    - 코드가 아니라, 설정 정보를 사용하기 때문에 코드를 고칠 수 없는 외부 라이브러리에도 초기화, 종료 메소드를 적용할 수 있다.
- <b>종료 메소드 추론</b>
    - `@Bean의 destroyMethod` 속성에는 특별한 기능이 있다.
    - 라이브러리 대부분 `close`, `shutdown` 이라는 이름의 종료 메소드를 사용한다.
    - @Bean의 `destroyMethod`는 기본값이 `(inferred)`(추론)으로 등록되어 있다.
    - 이 추론 기능은 `close`, `shutdown` 이라는 이름의 메소드를 자동으로 호출해준다.
        - 이름 그대로 종료 메소드를 추론해서 호출해준다.
    - 따라서 직접 스프링 빈으로 등록하면, 종료 메소드는 따로 적어주지 않아도 잘 동작한다.
    - 추론 기능을 사용하기 싫다면, `destroyMethod=""`처럼 빈 공백을 지정하면 된다.
___
## 애노테이션 @PostConstruct, @PreDestroy
```java
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class NetworkClient {

    private String url;

    public NetworkClient() {
        System.out.println("생성자 호출, url = " + url);
    }

    public void setUrl(String url) {
        this.url = url;
    }

    // 서비스 시작 시 호출
    public void connect(){
        System.out.println("connect: " + url);
    }

    public void call(String message){
        System.out.println("call: " + url + " message = " + message);
    }
    
    // 서비스 종료 시 호출
    public void disconnect(){
        System.out.println("close " + url);
    }

    @PostConstruct
    public void init() {
        System.out.println("NetworkClient.init");
        connect();
        call("초기화 연결 메세지");
    }

    @PreDestroy
    public void close() {
        System.out.println("NetworkClient.close");
        disconnect();
    }
}
```
```java
@Configuration
static class LifeCycleConfig {
    @Bean
    public NetworkClient networkClient(){
        NetworkClient networkClient = new NetworkClient();
        networkClient.setUrl("http://hello-spring.dev");
        return networkClient;
    }
}
```
- <b>실행 결과</b>
```
생성자 호출, url = null
NetworkClient.init
connect: http://hello-spring.dev
call: http://hello-spring.dev message = 초기화 연결 메시지
13:33:10.029 [main] DEBUG 
org.springframework.context.annotation.AnnotationConfigApplicationContext - Closing NetworkClient.close
close + http://hello-spring.dev
```
- `@PostConstruct`, `@PreDestroy` 이 두 애노테이션을 사용하면 가장 편리하게 초기화와 종료를 실행할 수 있다.<br><br>
- <b>@PostConstruct, @PreDestroy 애노테이션 특징</b>
    - 최신 스프링에서 가장 권장하는 방법이다.
    - 애노테이션 하나만 붙이면 되므로 매우 편리하다.
    - 패키지를 보면, `javax.annotation.PostConstruct`이다.
        - 스프링에 종속적인 기술이 아닌, JSR-250 이라는 자바 표준이다.
        - 따라서 스프링이 아닌 다른 컨테이너에서도 동작한다.
    - 컴포넌트 스캔과 잘 어울린다.
    - 유일한 단점은 외부 라이브러리에는 적용하지 못한다는 것이다.
        - 외부 라이브러리를 초기화, 종료해야 하면 @Bean의 기능을 사용하자.