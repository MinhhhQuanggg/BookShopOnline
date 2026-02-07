package fit.hutech.spring.controllers;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoHandlerFoundException.class)
    public String handle404() {
        return "error/404";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handle403() {
        return "error/403";
    }

    @ExceptionHandler(Exception.class)
    public String handle500() {
        return "error/500";
    }
}