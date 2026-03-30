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

        books.add(Book.builder()
                .id(1L)
                .title("Lap trinh Web Spring Framework")
                .author("Anh Nguyen")
                .price(29.99)
                .stock(100)
                .build());

        books.add(Book.builder()
                .id(2L)
                .title("Lap trinh ung dung Java")
                .author("Huy Cuong")
                .price(45.63)
                .stock(100)
                .build());

        books.add(Book.builder()
                .id(3L)
                .title("Lap trinh Web Spring Boot")
                .author("Xuan Nhan")
                .price(12.0)
                .stock(100)
                .build());

        books.add(Book.builder()
                .id(4L)
                .title("Lap trinh Web Spring MVC")
                .author("Anh Nguyen")
                .price(12.0)
                .stock(100)
                .build());

        books.add(Book.builder()
                .id(5L)
                .title("Think Java: How to Think Like a Computer Scientist")
                .author("Allen B. Downey and Chris Mayfield")
                .price(858.148)
                .stock(100)
                .build());

        return books;
    }
}