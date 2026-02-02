package fit.hutech.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // Phân quyền request
                .authorizeHttpRequests(auth -> auth
                        // Cho phép truy cập tài nguyên tĩnh
                        .requestMatchers("/css/bootstrap.min.css","/css/style.css", "/js/bootstrap.min.js", "/js/jquery-3.7.0.min.js").permitAll()
                        // Các request khác phải login
                        .anyRequest().authenticated()
                )

                // Login form mặc định
                .formLogin(login -> login
                        // Sau khi login thành công → vào /books
                        .defaultSuccessUrl("/books", true)
                        .permitAll()
                )

                // Logout mặc định
                .logout(Customizer.withDefaults());

        return http.build();
    }
}
