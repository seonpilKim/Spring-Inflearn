package hello.servlet.web.frontcontroller.v4;

import java.util.Map;

public interface ControllerV4 {

    // /** + enter하면 아래와 같이 나온다.
    /**
     *
     * @param paramMap
     * @param model
     * @return viewName
     */
    String process(Map<String, String> paramMap, Map<String, Object> model);
}
