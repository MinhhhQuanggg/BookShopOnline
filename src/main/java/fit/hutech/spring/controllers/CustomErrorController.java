package fit.hutech.spring.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Custom error controller Ã„â€˜Ã¡Â»Æ’ xÃ¡Â»Â­ lÃƒÂ½ access denied vÃƒÂ  cÃƒÂ¡c lÃ¡Â»â€”i khÃƒÂ¡c
 */
@Controller
public class CustomErrorController {

    @RequestMapping("/error/403")
    public String handleAccessDenied() {
        return "error/403";
    }
}
