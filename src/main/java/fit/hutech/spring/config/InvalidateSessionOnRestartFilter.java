package fit.hutech.spring.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class InvalidateSessionOnRestartFilter extends OncePerRequestFilter {
    private static final String SESSION_INSTANCE_ID_KEY = "APP_INSTANCE_ID";

    private final InstanceIdProvider instanceIdProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String currentInstanceId = instanceIdProvider.getInstanceId();
            Object sessionInstanceId = session.getAttribute(SESSION_INSTANCE_ID_KEY);

            if (sessionInstanceId == null) {
                session.setAttribute(SESSION_INSTANCE_ID_KEY, currentInstanceId);
            } else if (!currentInstanceId.equals(sessionInstanceId.toString())) {
                try {
                    session.invalidate();
                } catch (IllegalStateException ignored) {
                }
                SecurityContextHolder.clearContext();
                expireSessionCookie(request, response);
            }
        }

        filterChain.doFilter(request, response);
    }

    private static void expireSessionCookie(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie("JSESSIONID", "");
        String contextPath = request.getContextPath();
        cookie.setPath((contextPath == null || contextPath.isBlank()) ? "/" : contextPath);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}

