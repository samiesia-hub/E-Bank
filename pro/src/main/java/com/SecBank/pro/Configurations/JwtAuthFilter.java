package com.SecBank.pro.Configurations;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lit le token JWT depuis le header "Authorization: Bearer <token>" ou depuis
 * le cookie httpOnly "session", puis authentifie la requête si le token est valide.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIXE = "Bearer ";
    private static final String COOKIE_SESSION = "session";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = lireToken(request);

        if (token != null && jwtService.estValide(token)) {
            String email = jwtService.extraireEmail(token);
            String role = jwtService.extraireRole(token);

            List<SimpleGrantedAuthority> autorites = role == null
                    ? List.of()
                    : List.of(new SimpleGrantedAuthority("ROLE_" + role));

            var authentification = new UsernamePasswordAuthenticationToken(email, null, autorites);
            authentification.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentification);
        }

        filterChain.doFilter(request, response);
    }

    private String lireToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(PREFIXE)) {
            return header.substring(PREFIXE.length());
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_SESSION.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
