package com.enset.incidents.security;

import com.enset.incidents.entity.User;
import com.enset.incidents.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtre execute a chaque requete : lit "Authorization: Bearer <JWT>", verifie le token
 * et place l'utilisateur authentifie dans le SecurityContext.
 * Si le token est absent/invalide/expire, on ne fait rien : Spring Security renverra 401.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String ERROR_ATTRIBUTE = "jwt_error";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(7).trim();
            try {
                String email = jwtService.extractEmail(token);
                User user = userRepository.findByEmail(email).orElse(null);

                if (user == null) {
                    request.setAttribute(ERROR_ATTRIBUTE, "invalid");
                } else if (!user.isEnabled()) {
                    // Compte desactive apres l'emission du token : acces refuse immediatement
                    request.setAttribute(ERROR_ATTRIBUTE, "disabled");
                } else {
                    // Le role utilise pour les autorisations est celui de la base (source de verite)
                    var authentication = new UsernamePasswordAuthenticationToken(
                            user.getEmail(), null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (ExpiredJwtException e) {
                request.setAttribute(ERROR_ATTRIBUTE, "expired");
            } catch (JwtException | IllegalArgumentException e) {
                request.setAttribute(ERROR_ATTRIBUTE, "invalid");
            }
        }
        chain.doFilter(request, response);
    }
}
