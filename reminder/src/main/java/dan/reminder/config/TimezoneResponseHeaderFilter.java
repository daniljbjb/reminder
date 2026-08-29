package dan.reminder.config;

import dan.reminder.service.TimezoneResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TimezoneResponseHeaderFilter extends OncePerRequestFilter {

    private static final String TIMEZONE_HEADER = "X-Timezone";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String timezone = request.getHeader(TIMEZONE_HEADER);
        if (timezone == null || timezone.isBlank()) {
            response.setHeader("X-Timezone-Applied", TimezoneResolver.DEFAULT_ZONE.getId());
            response.setHeader("X-Timezone-Defaulted", "true");
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/reminder");
    }
}
