package fit.hutech.spring.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Custom error controller để xử lý access denied và các lỗi khác
 */
@Controller
public class CustomErrorController {

    @RequestMapping("/error/403")
    public String handleAccessDenied() {
        return "error/403";
    }
}
