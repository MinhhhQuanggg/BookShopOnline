package fit.hutech.spring.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class HomeController {

    @GetMapping("/")
    public String welcome() {
        return "home/index";
    }

    @GetMapping("/home")
    public String home() {
        return "home/index"; // trang sau login
    }

    // @GetMapping("/login")
    // public String login() {
    // return "user/login";
    // }
}
