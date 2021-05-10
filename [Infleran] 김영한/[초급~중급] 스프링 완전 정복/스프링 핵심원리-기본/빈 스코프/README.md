# 빈 스코프
## 목차
- 빈 스코프란?
- 프로토타입 스코프
- 프로토타입 스코프 - 싱글톤 빈과 함께 사용 시 문제점
- 프로토타입 스코프 - 싱글톤 빈과 함께 사용 시 Provider로 문제 해결
- 웹 스코프
- request 스코프 예제 만들기
- 스코프와 Provider
- 스코프와 프록시
___
## 빈 스코프란?
- 지금까지 우리는 스프링 빈이 스프링 컨테이너의 시작과 함께 생성되어서 스프링 컨테이너가 종료될 때 까지 유지된다고 학습했다.
    - 이것은 스프링 빈이 기본적으로 싱글톤 스코프로 생성되기 때문이다.
    - 스코프는 번역 그대로 빈이 존재할 수 있는 범위를 뜻한다.
- <b>스프링은 다음과 같은 다양한 스코프를 지원한다.</b>
    - <b>싱글톤</b>: 기본 스코프, 스프링 컨테이너의 시작과 종료까지 유지되는 가장 넓은 범위의 스코프이다.
    - <b>프로토타입</b>: 스프링 컨테이너는 프로토타입 빈의 생성과 의존관계 주입까지만 관여하고 더는 관리하지 않는 매우 짧은 범위의 스코프이다.
    - <b>웹 관련 스코프</b>
        - <b>request</b>: 웹 요청이 들어오고 나갈 때 까지 유지되는 스코프이다.
        - <b>session</b>: 웹 세션이 생성되고 종료될 때 까지 유지되는 스코프이다.
        - <b>application</b>: 웹의 서블릿 컨텍스트와 같은 범위로 유지되는 스코프이다.
- 빈 스코프는 다음과 같이 지정할 수 있다.<br><br>
- <b>컴포넌트 스캔 자동 등록</b>
```java
@Scope("prototype")
@Component
public class HelloBean {}
```
- <b>수동 등록</b>
```java
@Scope("prototype")
@Bean
PrototypeBean HelloBean() {
    return new HelloBean();
}
```
- 지금까지 싱글톤 스코프를 계속 사용해보았으니, 프로토타입 스코프부터 확인해보자.
___
## 프로토타입 스코프
- 싱글톤 스코프의 빈을 조회하면, 스프링 컨테이너는 항상 같은 인스턴스의 스프링 빈을 반환한다.
- 반면 프로토타입 스코프를 스프링 컨테이너에 조회하면 스프링 컨테이너는 항상 새로운 인스턴스를 생성해서 반환한다.<br><br>
- <b>싱글톤 빈 요청</b>
    - ![](imgs/1.PNG)
    1. 싱글톤 스코프의 빈을 스프링 컨테이너에 요청한다.
    2. 스프링 컨테이너는 본인이 관리하는 스프링 빈을 반환한다.
    3. 이후에 스프링 컨테이너에 같은 요청이 와도, 같은 객체 인스턴스의 스프링 빈을 반환한다.
- <b>프로토타입 빈 요청1</b>
    - ![](imgs/2.PNG)
    1. 프로토타입 스코프의 빈을 스프링 컨테이너에 요청한다.
    2. 스프링 컨테이너는 이 시점에 프로토타입 빈을 생성하고, 필요한 의존관계를 주입한다.
- <b>프로토타입 빈 요청2</b>
    - ![](imgs/3.PNG)
    3. 스프링 컨테이너는 생성한 프로토타입 빈을 클라이언트에 반환한다.
    4. 이후 스프링 컨테이너에 같은 요청이 오면 항상 새로운 프로토타입 빈을 생성해서 반환한다.
- <b>정리</b>
    - 여기서 <b>핵심은 스프링 컨테이너는 프로토타입 빈을 생성하고, 의존관계 주입, 초기화까지만 처리한다는 것</b>이다.
    - 클라이언트에 빈을 반환하고, 이후 스프링 컨테이너는 생성된 프로토타입 빈을 관리하지 않는다.
    - 프로토타입 빈을 관리할 책임은 프로토타입 빈을 받은 클라이언트에 있다.
        - 그래서 `@PreDestroy`같은 종료 메소드가 호출되지 않는다.
- <b>싱글톤 스코프 빈 테스트</b>
```java
public class SingletonTest {

    @Test
    void singletonBeanFind() {
        AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(SingletonBean.class);

        SingletonBean singletonBean = ac.getBean(SingletonBean.class);
        SingletonBean singletonBean1 = ac.getBean(SingletonBean.class);
        System.out.println("singletonBean = " + singletonBean);
        System.out.println("singletonBean1 = " + singletonBean1);
        assertThat(singletonBean).isSameAs(singletonBean1);

        ac.close(); // 종료
    }

    @Scope("singleton")
    static class SingletonBean{
        @PostConstruct
        public void init(){
            System.out.println("SingletonBean.init");
        }

        @PreDestroy
        public void destroy(){
            System.out.println("SingletonBean.destroy");
        }
    }
}
```
- 먼저 싱글톤 스코프의 빈을 조회하는 `singletonBeanFind()` 테스트를 실행해보자.<br><br>
- <b>실행 결과</b>
```
SingletonBean.init
singletonBean1 = hello.core.scope.PrototypeTest$SingletonBean@54504ecd
singletonBean2 = hello.core.scope.PrototypeTest$SingletonBean@54504ecd
org.springframework.context.annotation.AnnotationConfigApplicationContext - Closing SingletonBean.destroy
```
- 빈 초기화 메소드를 실행하고,
- 같은 인스턴스의 빈을 조회하고,
- 종료 메소드까지 정상 호출된 것을 확인할 수 있다.<br><br>
- <b>프로토타입 스코프 빈 테스트</b>
```java
public class PrototypeTest {

    @Test
    void prototypeBeanFind(){
        AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(PrototypeBean.class);

        System.out.println("find prototypeBean");
        PrototypeBean prototypeBean = ac.getBean(PrototypeBean.class);
        System.out.println("find prototypeBean1");
        PrototypeBean prototypeBean1 = ac.getBean(PrototypeBean.class);

        System.out.println("prototypeBean = " + prototypeBean);
        System.out.println("prototypeBean1 = " + prototypeBean1);

        assertThat(prototypeBean).isNotSameAs(prototypeBean1);

        ac.close(); // 종료
    }

    @Scope("prototype")
    static class PrototypeBean{
        @PostConstruct
        public void init(){
            System.out.println("PrototypeBean.init");
        }

        @PreDestroy
        public void destroy(){
            System.out.println("SingletonBean.destroy");
        }
    }
}
```
- 프로토타입 스코프의 빈을 조회하는 `prototypeBeanFind()` 테스트를 실행해보자.<br><br>
- <b>실행 결과</b>
```
find prototypeBean1
PrototypeBean.init
find prototypeBean2
PrototypeBean.init
prototypeBean1 = hello.core.scope.PrototypeTest$PrototypeBean@13d4992d
prototypeBean2 = hello.core.scope.PrototypeTest$PrototypeBean@302f7971
org.springframework.context.annotation.AnnotationConfigApplicationContext - Closing
```
- 싱글톤 빈은 스프링 컨테이너 생성 시점에 초기화 메소드가 실행되지만, 프로토타입 스코프의 빈은 스프링 컨테이너에서 빈을 조회할 때 생성되고, 초기화 메소드도 실행된다.
- 프로토타입 빈을 2번 조회했으므로, 완전히 다른 스프링 빈이 생성되고, 초기화도 2번 실행된 것을 확인할 수 있다.
- 싱글톤 빈은 스프링 컨테이너가 관리하기 때문에, 스프링 컨테이너가 종료될 때 빈의 종료 메소드가 실행되지만, 프로토타입 빈은 스프링 컨테이너가 생성과 의존관계 주입, 그리고 초기화 까지만 관여하고, 더는 관리하지 않는다.
    - 따라서 프로토타입 빈은 스프링 컨테이너가 종료될 때 `@PreDestory`같은 종료 메소드가 전혀 실행되지 않는다.
- <b>프로토타입 빈의 특징 정리</b>
    - 스프링 컨테이너에 요청할 때 마다 새로 생성된다.
    - 스프링 컨테이너는 프로토타입 빈의 생성과 의존관계 주입, 그리고 초기화까지만 관여한다.
    - 종료 메소드가 호출되지 않는다.
    - 그래서 프로토타입 빈은 프로토타입 빈을 조회한 클라이언트가 관리해야 한다.
        - 종료 메소드에 대한 호출도 클라이언트가 직접 해야한다.
___
## 프로토타입 스코프 - 싱글톤 빈과 함께 사용 시 문제점
- 스프링 컨테이너에 프로토타입 스코프의 빈을 요청하면, 항상 새로운 객체 인스턴스를 생성해서 반환한다.
- 하지만 싱글톤 빈과 함께 사용할 때는 의도한대로 잘 동작하지 않으므로 주의해야 한다.<br><br>
- 먼저 스프링 컨테이너에 프로토타입 빈을 직접 요청하는 예제를 보자.
### 프로토타입 빈 직접 요청
- <b>스프링 컨테이너에 프로토타입 빈 직접 요청1</b>
    - ![](imgs/4.PNG)
    1. 클라이언트A는 스프링 컨테이너에 프로토타입 빈을 요청한다.
    2. 스프링 컨테이너는 프로토타입 빈을 새로 생성해서 반환(<b>x01</b>)한다. 해당 빈의 count 필드 값은 0이다.
    3. 클라이언트는 조회한 프로토타입 빈에 `addCount()`를 호출하면서 count 필드를 +1 한다.
    - 결과적으로 프로토타입 빈(<b>x01</b>)의 count는 1이 된다.
- <b>스프링 컨테이너에 프로토타입 빈 직접 요청2</b>
    - ![](imgs/5.PNG)
    1. 클라이언트B는 스프링 컨테이너에 프로토타입 빈을 요청한다.
    2. 스프링 컨테이너는 프로토타입 빈을 새로 생성해서 반환(<b>x02</b>)한다. 해당 빈의 count 필드 값은 0이다.
    3. 클라이언트는 조회한 프로토타입 빈에 `addCount()`를 호출하면서 count 필드를 +1 한다.
    - 결과적으로 프로토타입 빈(<b>x02</b>)의 count는 1이 된다.
- <b>코드로 확인</b>
```java
public class SingletonWithPrototypeTest1 {

    @Test
    void prototypeFind() {
        AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(PrototypeBean.class);

        PrototypeBean prototypeBean = ac.getBean(PrototypeBean.class);
        prototypeBean.addCount();
        assertThat(prototypeBean.getCount()).isEqualTo(1);

        PrototypeBean prototypeBean1 = ac.getBean(PrototypeBean.class);
        prototypeBean1.addCount();
        assertThat(prototypeBean1.getCount()).isEqualTo(1);
    }

    @Scope("prototype")
    static class PrototypeBean {
        private int count = 0;

        public void addCount() {
            count++;
        }

        public int getCount() {
            return count;
        }

        @PostConstruct
        public void init() {
            System.out.println("PrototypeBean.init = " + this);
        }

        @PreDestroy
        public void destroy() {
            System.out.println("PrototypeBean.destroy");
        }
    }
}
```
### 싱글톤 빈에서 프로토타입 빈 사용
- 이번에는 `clientBean` 이라는 싱글톤 빈이 의존관계 주입을 통해 프로토타입 빈을 주입받아 사용하는 예를 보자.<br><br>
- <b>싱글톤에서 프로토타입 빈 사용1</b>
    - ![](imgs/6.PNG)
    - `clientBean`은 싱글톤이므로, 보통 스프링 컨테이너 생성 시점에 함께 생성되고, 의존관계 주입도 발생한다.
    1. `clientBean`은 의존관계 자동 주입을 사용한다. 주입 시점에 스프링 컨테이너에 프로토타입 빈을 요청한다.
    2. 스프링 컨테이너는 프로토타입 빈을 생성해서 `clientBean`에 반환한다. 프로토타입 빈의 count 필드 값은 0이다.
    3. 이제 `clientBean`은 프로토타입 빈을 내부 필드에 보관한다. (정확히는 참조값을 보관한다.)
- <b>싱글톤에서 프로토타입 빈 사용2</b>
    - ![](imgs/7.PNG)
    - 클라이언트A는 `clientBean`을 스프링 컨테이너에 요청해서 받는다. 싱글톤이므로 항상 같은 `clientBean`이 반환된다.
    3. 클라이언트A는 `clientBean.logic()`을 호출한다.
    4. `clientBean`은 prototypeBean의 `addCount()`를 호출해서 프로토타입 빈의 count를 증가시킨다. count의 값이 1이 된다.
- <b>싱글톤에서 프로토타입 빈 사용3</b>
    - ![](imgs/8.PNG)
    - 클라이언트B는 `clientBean`을 스프링 컨테이너에 요청해서 받는다. 싱글톤이므로 항상 같은 `clientBean`이 반환된다.
    - <b>여기서 중요한 점이 있는데, clientBean이 내부에 가지고 있는 프로토타입 빈은 이미 과거에 주입이 끝난 빈이다. 주입 시점에 스프링 컨테이너에 요청해서 프로토타입 빈이 새로 생성이 된 것이지, 사용할 때마다 새로 생성되는 것이 아니다!</b>
    5. 클라이언트B는 `clientBean.logic()`을 호출한다.
    6. `clientBean`은 prototypeBean의 `addCOunt()`를 호출해서 프로토타입 빈의 count를 증가시킨다. 원래 count의 값이 1이었으므로, 2가 된다.
- <b>테스트 코드</b>
```java
public class SingletonWithPrototypeTest1 {

    @Test
    void singletonClientUsePrototype() {
        AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(ClientBean.class, PrototypeBean.class);

        ClientBean clientBean = ac.getBean(ClientBean.class);
        int count = clientBean.logic();
        assertThat(count).isEqualTo(1);

        ClientBean clientBean1 = ac.getBean(ClientBean.class);
        int count1 = clientBean1.logic();
        assertThat(count1).isEqualTo(2);
    }

    //@Scope("singleton") 생략 가능
    static class ClientBean {
        private final PrototypeBean prototypeBean;

        @Autowired
        public ClientBean(PrototypeBean prototypeBean) {
            this.prototypeBean = prototypeBean;
        }

        public int logic() {
            prototypeBean.addCount();
            int count = prototypeBean.getCount();
            return count;
        }
    }

    @Scope("prototype")
    static class PrototypeBean {
        private int count = 0;

        public void addCount() {
            count++;
        }

        public int getCount() {
            return count;
        }

        @PostConstruct
        public void init() {
            System.out.println("PrototypeBean.init = " + this);
        }

        @PreDestroy
        public void destroy() {
            System.out.println("PrototypeBean.destroy");
        }
    }
}
```
- 스프링은 일반적으로 싱글톤 빈을 사용하므로, 싱글톤 빈이 프로토타입 빈을 사용하게 된다.
    - 그런데 싱글톤 빈은 생성 시점에만 의존관계 주입을 받기 때문에, 프로토타입 빈이 새로 생성되기는 하지만, 싱글톤 빈과 함께 계속 유지되는 것이 문제다.
- 그러나, 의도한 것은 프로토타입 빈을 주입 시점에만 새로 생성하는 것이 아니라, 사용할 때마다 새로 생성해서 사용하는 것이다.
> <b>참고</b>
> - 여러 빈에서 같은 프로토타입 빈을 주입받으면, <b>주입 받는 시점에 각각 새로운 프로토타입 빈이 생성</b>된다.
> - 예를 들어, clientA, clientB가 각각 의존관계 주입을 받으면, 각각 다른 인스턴스의 프로토타입 빈을 주입받는다.
> - clientA -> prototypeBean@x01
> - cleintB -> prototypeBean@x02
> - 물론 사용할 때마다 새로 생성되는 것은 아니다.
___
## 프로토타입 스코프 - 싱글톤 빈과 함께 사용 시 Provider로 문제 해결
- 싱글톤 빈과 프로토타입 빈을 함께 사용할 때, 어떻게 하면 사용할 때마다 항상 새로운 프로토타입 빈을 생성할 수 있을까?
### 스프링 컨테이너에 요청
- 가장 간단한 방법은 싱글톤 빈이 프로토타입을 사용할 때마다 스프링 컨테이너에 새로 요청하는 것이다.
```java
public class PrototypeProviderTest {
    @Test
    void providerTest() {

        AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(ClientBean.class, PrototypeBean.class);

        ClientBean clientBean1 = ac.getBean(ClientBean.class);
        int count1 = clientBean1.logic();
        assertThat(count1).isEqualTo(1);

        ClientBean clientBean2 = ac.getBean(ClientBean.class);
        int count2 = clientBean2.logic();
        assertThat(count2).isEqualTo(1);
    }
    // 핵심 코드 ----------------------------------------------------------
    static class ClientBean {

        @Autowired
        private ApplicationContext ac;

        public int logic() {
            PrototypeBean prototypeBean = ac.getBean(PrototypeBean.class);
            prototypeBean.addCount();
            int count = prototypeBean.getCount();
            return count;
        }
    } 
    // 핵심 코드 ----------------------------------------------------------
    @Scope("prototype")
    static class PrototypeBean {

        private int count = 0;

        public void addCount() {
            count++;
        }

        public int getCount() {
            return count;
        }

        @PostConstruct
        public void init() {
            System.out.println("PrototypeBean.init " + this);
        }

        @PreDestroy
        public void destroy() {
            System.out.println("PrototypeBean.destroy");
        }
    }
}
```
- 실행해보면 핵심코드의 `ac.getBean()`을 통해 항상 새로운 프로토타입 빈이 생성되는 것을 확인할 수 있다.
    - 의존관계를 외부에서 주입(DI)받는 게 아니라, 이렇게 직접 필요한 의존관계를 찾는 것을 **Dependency Lookup(DL)** 의존관계 조회(탐색)이라 한다.
- 그런데 이렇게 스프링 애플리케이션 컨텍스트 전체를 주입받게 되면, 스프링 컨테이너에 종속적인 코드가 되고, 단위 테스트도 어려워진다.
- 지금 필요한 기능은 지정한 프로토타입 빈을 컨테이너에서 대신 찾아주는 딱! <b>DL</b>정도의 기능만 제공하는 무언가가 있으면 된다.<br><br>
- 스프링에는 이미 모든 게 준비되어 있다.
### ObjectFactory, ObjectProvider
- 지정한 빈을 컨테이너에서 대신 찾아주는 DL 서비스를 제공하는 것이 바로 `ObjectProvider`이다.
- 참고로 과거에는 `ObjectFactorcy`가 있었는데, 여기에 편의 기능들을 추가하여 `ObjectProvider`가 만들어졌다.
```java
static class ClientBean {

    @Autowired
    private ObjectProvider<PrototypeBean> prototypeBeanProvider;

    public int logic() {
        PrototypeBean prototypeBean = prototypeBeanProvider.getObject();
        prototypeBean.addCount();
        int count = prototypeBean.getCount();
        return count;
    }
}
```
- 실행해보면 `prototypeBeanProvider.getObject()`을 통해 항상 새로운 프로토타입 빈이 생성되는 것을 확인할 수 있다.
- `ObjectProvider`의 `getObject()`를 호출하면, 내부에서는 스프링 컨테이너를 통해 해당 빈을 찾아 반환한다. (<b>DL</b>)
- 스프링이 제공하는 기능을 사용하지만, 기능이 단순하므로 단위테스트를 만들거나 mock 코드를 만들기는 훨씬 쉬워진다.
- `ObjectProvider`는 지금 딱 필요한 DL 정도의 기능만 제공한다.<br><br>
- <b>특징</b>
    - ObjectFactory: 기능이 단순, 별도의 라이브러리 필요 없음, 스프링에 의존
    - ObjectProvider: ObjectFactory 상속, 옵션, 스트림 처리 등 편의 기능이 많으며 별도의 라이브러리가 필요 없음, 스프링에 의존
### JSR-330 Provider
- 마지막 방법은 `javax.inject.Provider`라는 JSR-330 자바 표준을 사용하는 방법이다.
- 이 방법을 사용하려면 `javax.inject:javax.inject:1` 라이브러리를 gradle에 추가해야 한다.<br><br>
- <b>javax.inject.Provider 참고용 코드</b>
```java
package javax.inject;
public interface Provider<T> {
    T get();
}
```
```java
//implementation 'javax.inject:javax.inject:1' build.gradle에 추가 필수
@Autowired
private Provider<PrototypeBean> provider;

public int logic() {
    PrototypeBean prototypeBean = provider.get();
    prototypeBean.addCount();
    int count = prototypeBean.getCount();
    return count;
}
```
- 실행해보면 `provider.get()`을 통해 항상 새로운 프로토타입 빈이 생성되는 것을 확인할 수 있다.
- `provider`의 `get()`을 호출하면, 내부에서는 스프링 컨테이너를 통해 해당 빈을 찾아 반환한다. (<b>DL</b>)
- 자바 표준이고, 기능이 단순하므로 단위테스트를 만들거나, mock 코드를 만들기 훨씬 쉬워진다.
- `Provider`는 지금 딱 필요한 DL 정도의 기능만 제공한다.<br><br>
- <b>특징</b>
    - `get()` 메소드 하나로 기능이 매우 단순하다.
    - 별도의 라이브러리가 필요하다.
    - 자바 표준이므로 스프링이 아닌 다른 컨테이너에서도 사용할 수 있다.
### 정리
- 그러면 프로토타입 빈을 언제 사용할까?
    - 매번 사용할 때마다 의존관계 주입이 완료된 새로운 객체가 필요할 때 사용하면 된다.
    - 그런데 실무에서 웹 애플리케이션을 개발해보면, 싱글톤 빈으로 대부분의 문제를 해결할 수 있기 때문에 프로토타입 빈을 직접적으로 사용하는 일은 매우 드물다고 한다.
- `ObjectProvider`, `JSR330 Provider` 등은 프로토타입 뿐만 아니라 DL이 필요한 경우에도 언제든지 사용이 가능하다.
> <b>참고</b>
> - 스프링이 제공하는 메소드에 `@Lookup` 애노테이션을 사용하는 방법도 있지만, 이전 방법들로 충분하고, 고려해야 할 내용도 많아서 생략한다.

> <b>참고</b>
> - 실무에서 자바 표준인 JSR-330 Provider를 사용할 것인지, 스프링이 제공하는 ObjectProvider를 사용할 것인지 고민이 될 것이다.
> - ObjectProvider는 DL을 위한 편의 기능을 많이 제공해주고, 스프링 외에 별도의 의존관계 추가가 필요 없기 때문에 편리하다.
> - 만약 코드를 스프링이 아닌 다른 컨테이너에서도 사용할 수 있어야 한다면, JSR-330 Provider를 사용해야 한다.

> - 스프링을 사용하다 보면 이 기능 뿐만 아니라, 다른 기능들도 자바 표준과 스프링이 제공하는 기능이 겹칠 때가 많이 있다.
> - 대부분 스프링이 더 다양하고 편리한 기능을 제공해주기 때문에, 특별히 다른 컨테이너를 사용할 일이 없다면, 스프링이 제공하는 기능을 사용하자.
___
## 웹 스코프
- 지금까지 싱글톤과 프로토타입 스코프를 학습했다.
- 싱글톤은 스프링 컨테이너의 시작과 끝까지 함께하는 매우 긴 스코프이고, 프로토타입은 생성과 의존관계 주입, 그리고 초기화까지만 진행하는 특별한 스코프이다.<br><br>
- 이번에는 웹 스코프에 대해 알아보자.<br><br>
- <b>웹 스코프의 특징</b>
    - 웹 스코프는 웹 환경에서만 동작한다.
    - 웹 스코프는 프로토타입과 다르게, 스프링이 해당 스코프의 종료시점까지 관리한다.
        - 따라서 종료 메소드가 호출된다.
- <b>웹 스코프 종류</b>
    - <b>request:</b> HTTP 요청 하나가 들어오고, 나갈 때 까지 유지되는 스코프로, 각각의 HTTP 요청마다 별도의 빈 인스턴스가 생성되고, 관리된다.
    - <b>session:</b> HTTP Session과 동일한 생명주기를 갖는 스코프
    - <b>application:</b> 서블릿 컨텍스트(`ServletContext`)와 동일한 생명주기를 갖는 스코프
    - <b>websocket:</b> 웹 소켓과 동일한 생명주기를 갖는 스코프
- <b>HTTP request 요청당 각각 할당되는 request 스코프</b>
    - ![](imgs/9.PNG)
___
## request 스코프 예제 만들기
### 웹 환경 추가
- 웹 스코프는 웹 환경에서만 동작하므로, web 환경이 동작하도록 라이브러리를 추가하자.<br><br>
- <b>build.gradle에 추가</b>
```gradle
//web 라이브러리 추가
implementation 'org.springframework.boot:spring-boot-starter-web'
```
- 이제 `hello.core.CoreApplication`의 main 메소드를 실행하면, 웹 애플리케이션이 실행되는 것을 확인할 수 있다.
```
Tomcat started on port(s): 8080 (http) with context path ''
Started CoreApplication in 0.914 seconds (JVM running for 1.528)
```
> <b>참고</b>
> - `spring-boot-starter-web` 라이브러리를 추가하면 스프링 부트는 내장 톰켓 서버를 활용하여 웹 서버와 스프링을 함께 실행시킨다.
> - 스프링 부트는 웹 라이브러리가 없으면, 지금까지 학습한 `AnnotationConfigApplicationContext`를 기반으로 애플리케이션을 구동한다.
> - 웹 라이브러리가 추가되면, 웹과 관련된 추가 설정과 환경들이 필요하므로 `AnnotationConfigApplicationContext`를 기반으로 애플리케이션을 구동한다.
- 만약 기본 포트인 8080 포트를 다른 곳에서 사용 중이어서 오류가 발생하면 포트를 변경해야 한다.
    - 9090 포트로 변경하려면 다음 설정을 추가하자.
        - `main/resources/application.properties`
            ```
            server.port=9090
            ```
### request 스코프 예제 개발
- 동시에 여러 HTTP 요청이 오면, 정확히 어떤 요청이 남긴 로그인지 구분하기 어렵다.
- 이럴 때 사용하기 좋은 것이 바로 request 스코프이다.<br><br>
- 다음과 같이 로그가 남도록 request 스코프를 활용하여 추가 기능을 개발해보자.
```
[d06b992f...] request scope bean create
[d06b992f...][http://localhost:8080/log-demo] controller test
[d06b992f...][http://localhost:8080/log-demo] service id = testId
[d06b992f...] request scope bean close
```
- 기대하는 공통 포멧: [UUID][requestURL]{message}
- UUID를 사용해서 HTTP 요청을 구분하자.
- requestURL 정보도 추가로 넣어 어떤 URL을 요청해서 남은 로그인지 확인하자.<br><br>
- <b>MyLogger</b>
```java
import java.util.UUID;

@Component
@Scope("request")
public class MyLogger {
    private String uuid;
    private String requestURL;

    public void setRequestURL(String requestURL) {
        this.requestURL = requestURL;
    }

    public void log(String message) {
        System.out.println("[" + uuid + "]" + "[" + requestURL + "] " + message);
    }

    @PostConstruct
    public void init() { 
        uuid = UUID.randomUUID().toString();
        System.out.println("[" + uuid + "] request scope bean create:" + this);
    }

    @PreDestroy
    public void close() {
        ystem.out.println("[" + uuid + "] request scope bean close:" + this);
    }
}
```
- 로그를 출력하기 위한 `MyLogger` 클래스이다.
- `@Scope("request")`를 사용해서 request 스코프로 지정했다.
    - 이제 이 빈은 HTTP 요청당 하나씩 생성되고, HTTP 요청이 끝나는 시점에 소멸된다.
- 이 빈이 생성되는 시점에 자동으로 `@PostConstruct` 초기화 메소드를 사용하여 uuid를 생성해서 저장해둔다.
    - 이 빈은 HTTP 요청당 하나씩 생성되므로, uuid를 저장해두면 다른 HTTP 요청과 구분할 수 있다.
- 이 빈이 소멸되는 시점에 `@PreDestroy`를 사용해서 종료 메세지를 남긴다.
- `requestURL`은 이 빈이 생성되는 시점에는 알 수 없으므로, 외부에서 setter로 입력받는다.<br><br>
- <b>LogDemoController</b>
```java
@Controller
@RequiredArgsConstructor
public class LogDemoController {

    private final LogDemoService logDemoService; 
    private final MyLogger myLogger;

    @RequestMapping("log-demo")
    @ResponseBody
    public String logDemo(HttpServletRequest request) {
        String requestURL = request.getRequestURL().toString();
        myLogger.setRequestURL(requestURL);

        myLogger.log("controller test");
        logDemoService.logic("testId");

        return "OK";
    }
}
```
- 로거가 잘 작동하는지 확인하는 테스트용 컨트롤러다.
- 여기서 HttpServletRequest를 통해 요청 URL을 받았다.
    - requestURL 값 `http://localhost:8080/log-demo`
- 이렇게 받은 requestURL 값을 myLogger에 저장해둔다.
    - myLogger는 HTTP 요청당 각각 구분되므로, 다른 HTTP 요청때문에 값이 섞이는 걱정은 하지 않아도 된다.
- 컨트롤러에서 controller test라는 로그를 남긴다.
> <b>참고</b>
> - requestURL을 MyLogger에 저장하는 부분은 컨트롤러보다는 공통 처리가 가능한 스프링 인터셉터나 서블릿 필터같은 곳을 활용하는 것이 좋다.
> - 여기서는 예제를 단순화하기 위해 컨트롤러를 사용했다.
- <b>LogDemoService 추가</b>
```java
@Service
@RequiredArgsConstructor
public class LogDemoService {

    private final MyLogger myLogger;

    public void logic(String Id) {
        myLogger.log("service id = " + Id);
    }
}
```
- 비즈니스 로직이 있는 서비스 계층에서도 로그를 출력해보자.
- 여기서 중요한 점이 있다. request scope를 사용하지 않고, 파라미터로 이 모든 정보를 서비스 계층에 넘긴다면, 파라미터가 많아 지저분하다.
    - 더 문제는 requestURL 같은 웹과 관련된 정보가 웹과 관련없는 서비스 계층까지 넘어가게 된다.
    - 웹과 관련된 부분은 컨트롤러까지만 사용해야 한다.
    - 서비스 계층은 웹 기술에 종속되지 않고, 가급적 순수하게 유지하는 것이 유지보수 관점에서 좋다.
- request scope의 MyLogger 덕분에 이런 부분을 파라미터로 넘기지 않고, MyLogger의 멤버변수에 저장해서 코드와 계층을 깔끔하게 유지할 수 있다.<br><br>
- <b>기대하는 출력</b>
```
[d06b992f...] request scope bean create
[d06b992f...][http://localhost:8080/log-demo] controller test
[d06b992f...][http://localhost:8080/log-demo] service id = testId
[d06b992f...] request scope bean close
```
- <b>실제는 기대와 다르게 애플리케이션 실행 시점에 오류가 발생한다.</b>
```
Error creating bean with name 'myLogger': Scope 'request' is not active for the current thread; consider defining a scoped proxy for this bean if you intend to refer to it from a singleton;
```
- 스프링 애플리케이션을 실행하는 시점에 싱글톤 빈은 생성해서 주입이 가능하지만, request 스코프 빈은 아직 생성되지 않는다.
    - 이 빈은 실제 고객의 요청이 와야 생성할 수 있다!
___
## 스코프와 Provider
- 첫 번째 해결방안은 앞서 배운 Provider를 사용하는 것이다.
- 간단히 ObjectProvider를 사용해보자.
```java
@Controller
@RequiredArgsConstructor
public class LogDemoController {

    private final LogDemoService logDemoService;
    private final ObjectProvider<MyLogger> myLoggerProvider;

    @RequestMapping("log-demo")
    @ResponseBody
    public String logDemo(HttpServletRequest request) {
        String requestURL = request.getRequestURL().toString();
        MyLogger myLogger = myLoggerProvider.getObject();
        myLogger.setRequestURL(requestURL);

        myLogger.log("controller test");
        logDemoService.logic("testId"); 

        return "OK";
    }
}
```
```java
@Service
@RequiredArgsConstructor
public class LogDemoService {

    private final ObjectProvider<MyLogger> myLoggerProvider;

    public void logic(String id) {
        MyLogger myLogger = myLoggerProvider.getObject();
        myLogger.log("service id = " + id);
    }
}
```
- `main()` 메소드로 스프링을 실행하고, 웹 브라우저에 `http://localhost:8080/log-demo`를 입력하자.<br><br>
- 드디어 잘 작동하는 것을 확인할 수 있다.
```
[d06b992f...] request scope bean create
[d06b992f...][http://localhost:8080/log-demo] controller test
[d06b992f...][http://localhost:8080/log-demo] service id = testId
[d06b992f...] request scope bean close
```
- `ObjectProvider` 덕분에 `ObjectProvider.getObject()`를 호출하는 시점까지 request scope <b>빈의 생성을 지연</b>할 수 있다.
- `ObjectProvider.getObject()`를 호출하는 시점에는 HTTP 요청이 진행중이므로 request scope 빈의 생성이 정상 처리된다.
- `ObjectProvider.getObject()`를 `LogDemoController`, `LogDemoService`에서 각각 한 번씩 따로 호출해도 같은 HTTP 요청이면 같은 스프링 빈이 반환된다!<br><br>
- 이 정도에서 끝내도 될 것 같지만... 개발자들의 코드 몇 자를 더 줄이려는 욕심은 끝이 없다.
___
## 스코프와 프록시
- 이번에는 프록시 방식을 사용해보자.
```java
@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class MyLogger {...}
```
- 여기가 핵심이다. `proxyMode = ScopedProxyMode.TARGET_CLASS`를 추가해주자.
    - 적용 대상이 클래스면 `TARGET_CLASS`를 선택
    - 적용 대상이 인터페이스면 `INTERFACES`를 선택
- 이렇게 하면 MyLogger의 가짜 프록시 클래스를 만들어두고 HTTP request와 상관없이 가짜 프록시 클래스를 다른 빈에 미리 주입해 둘 수 있다.<br><br>
- 이제 나머지 코드를 Provider 사용 이전으로 돌려두자.
```java
@Controller
@RequiredArgsConstructor
public class LogDemoController {

    private final LogDemoService logDemoService;
    private final MyLogger myLogger;

    @RequestMapping("log-demo")
    @ResponseBody
    public String logDemo(HttpServletRequest request){
        String requestURL = request.getRequestURL().toString();

        System.out.println("myLogger = " + myLogger.getClass());
        myLogger.setRequestURL(requestURL);

        myLogger.log("controller test");
        logDemoService.logic("testId");
        return "OK";
    }
}
```
```java
@Service
@RequiredArgsConstructor
public class LogDemoService {

    private final MyLogger myLogger;

    public void logic(String Id) {
        myLogger.log("service id = " + Id);
    }
}
```
- 실행해보면 잘 동작하는 것을 확인할 수 있다.<br><br>
- 코드를 잘 보면 `LogDemoController`, `LogDemoService`는 Provider 사용 전과 완전히 동일하다. 어떻게 된 것일까?
### 웹 스코프와 프록시 동작 원리
- 먼저 주입된 myLogger를 확인해보자.
```java
System.out.println("myLogger = " + myLogger.getClass());
```
- <b>출력결과</b>
```
myLogger = class hello.core.common.MyLogger$$EnhancerBySpringCGLIB$$b68b726d
```
- <b>CGLIB라는 라이브러리로 내 클래스를 상속받은 가짜 프록시 객체를 만들어서 주입한다.</b>
- `@Scope`의 `proxyMode = ScopedProxyMode.TARGET_CLASS`를 설정하면 스프링 컨테이너는 CGLIB라는 바이트 코드를 조작하는 라이브러리를 사용해서, MyLogger를 상속받은 가짜 프록시 객체를 생성한다.
- 결과를 확인해보면 순수한 MyLogger 클래스가 아닌, `MyLogger$$EnhancerBySpringCGLIB` 이라는 클래스로 만들어진 객체가 대신 등록된 것을 확인할 수 있다.
- 그리고 스프링 컨테이너에 "myLogger"라는 이름으로 진짜 대신에 이 가짜 프록시 객체를 등록한다.
- `ac.getBean("myLogger", MyLogger.class)`로 조회해도 프록시 객체가 조회되는 것을 확인할 수 있다.
- 그래서 의존관계 주입도 이 가짜 프록시 객체가 주입된다.
- ![](imgs/10.PNG)
- <b>가짜 프록시 객체는 요청이 오면, 그 때 내부에서 진짜 빈을 요청하는 위임 로직이 들어있다.</b>
    - 가짜 프록시 객체는 내부에 진짜 myLogger를 찾는 방법을 알고 있다.
    - 클라이언트가 `myLogger.logic()`을 호출하면, 사실 가짜 프록시 객체의 메소드를 호출한 것이다.
    - 가짜 프록시 객체는 request 스코프의 진짜 `myLogger.logic()`을 호출한다.
    - 가짜 프록시 객체는 원본 클래스를 상속받아 만들어졌기 때문에, 이 객체를 사용하는 클라이언트 입장에서는 사실 원보인지 아닌지도 모르게, 동일하게 사용할 수 있다.(다형성)
- <b>동작 정리</b>
    - CGLIB라는 라이브러리로 내 클래스를 상속받은 가짜 프록시 객체를 만들어서 주입한다.
    - 이 가짜 프록시 객체는 실제 요청이 오면, 그 때 내부에서 실제 빈을 요청하는 위임 로직이 들어있다.
    - 가짜 프록시 객체는 실제 request scope와는 관계가 없다.
        - 그냥 가짜이고, 내부에 단순한 위임 로직만 있으며 싱글톤처럼 동작한다.
- <b>특징 정리</b>
    - 프록시 객체 덕분에 클라이언트는 마치 싱글톤 빈을 사용하듯이 편리하게 request scope를 사용할 수 있다.
    - 사실 Provider를 사용하든, 프록시를 사용하든, 핵심 아이디어는 진짜 객체 조회를 꼭 필요한 시점까지 지연처리 한다는 점이다.
    - 단지 애노테이션 설정 변경만으로 원본 객체를 프록시 객체로 대체할 수 있다. 
        - 이것이 바로 다형성과 DI 컨테이너가 가진 큰 강점이다.
    - 꼭 웹 스코프가 아니어도, 프록시는 사용할 수 있다.
- <b>주의점</b>
    - 마치 싱글톤을 사용하는 것 같지만, 다르게 동작하기 때문에 결국 주의해서 사용해야 한다.
    - 이런 특별한 scope는 꼭 필요한 곳에서 최소화해서 사용하자, 무분별하게 사용하면 유지보수하기 어려워진다.