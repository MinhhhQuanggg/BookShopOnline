package fit.hutech.spring;

import fit.hutech.spring.entities.Book;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class AppConfig {

    @Bean
    public List<Book> getBooks() {
        List<Book> books = new ArrayList<>();

        books.add(new Book(
                1L,
                "Lập trình Web Spring Framework",
                "Ánh Nguyễn",
                29.99,
                "Công nghệ thông tin"
        ));

        books.add(new Book(
                2L,
                "Lập trình ứng dụng Java",
                "Huy Cường",
                45.63,
                "Công nghệ thông tin"
        ));

        books.add(new Book(
                3L,
                "Lập trình Web Spring Boot",
                "Xuân Nhân",
                12.0,
                "Công nghệ thông tin"
        ));

        books.add(new Book(
                4L,
                "Lập trình Web Spring MVC",
                "Ánh Nguyễn",
                12.0,
                "Công nghệ thông tin"
        ));

        books.add(new Book(
                5L,
                "Think Java: How to Think Like a Computer Scientist",
                "Allen B. Downey và Chris Mayfield",
                858.148,
                "Khoa học máy tính"
        ));

        return books;
    }
}