package hackzurich;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by radoslawjarzynka on 16.09.16.
 */
@RestController
public class TestController {

    private static final String msg = "Test %s!";

    @RequestMapping("/test")
    public TestBean testMethod(@RequestParam(value="parameter", defaultValue="Default parameter") String parameter) {
        return new TestBean(String.format(msg, parameter));
    }
}
