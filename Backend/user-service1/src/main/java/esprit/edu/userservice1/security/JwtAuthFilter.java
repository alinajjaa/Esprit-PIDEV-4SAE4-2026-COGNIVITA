package esprit.edu.userservice1.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();

        // OPTIONS toujours ignoré
        if (HttpMethod.OPTIONS.matches(method)) return true;

        // ✅ Routes OAuth2 — laisser Spring Security les gérer
        if (path.startsWith("/login/oauth2")) return true;
        if (path.startsWith("/oauth2"))       return true;
        if (path.equals("/login"))            return true;

        // Routes publiques classiques
        if (HttpMethod.POST.matches(method) && "/api/users".equals(path))                 return true;
        if (HttpMethod.POST.matches(method) && "/api/users/login".equals(path))           return true;
        if (HttpMethod.POST.matches(method) && "/api/users/forgot-password".equals(path)) return true;
        if (HttpMethod.POST.matches(method) && "/api/users/reset-password".equals(path))  return true;
        if (HttpMethod.GET.matches(method)  && "/api/users/by-email".equals(path))        return true;

        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String auth = request.getHeader("Authorization");

        if (auth == null || !auth.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = auth.substring(7);

        if (!jwtService.isValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String email = jwtService.extractEmail(token);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
