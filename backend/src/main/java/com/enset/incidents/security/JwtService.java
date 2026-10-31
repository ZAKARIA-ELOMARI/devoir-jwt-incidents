package com.enset.incidents.security;

import com.enset.incidents.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Creation et verification des JWT.
 * Claims : sub (email), role, iat (creation), exp (expiration).
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    public String generateToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getEmail())                       // email
                .claim("role", user.getRole().name())           // claim personnalise : role
                .issuedAt(now)                                  // date de creation
                .expiration(new Date(now.getTime() + expirationMs)) // date d'expiration
                .signWith(key, Jwts.SIG.HS256)                  // signature HMAC-SHA256
                .compact();
    }

    /**
     * Verifie la signature ET l'expiration. Leve une JwtException
     * (ExpiredJwtException, SignatureException, MalformedJwtException...) si le token n'est pas valide.
     */
    public Claims parse(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token) {
        return parse(token).getSubject();
    }

    public String extractRole(String token) {
        return parse(token).get("role", String.class);
    }

    public boolean isTokenExpired(String token) {
        try {
            return parse(token).getExpiration().before(new Date());
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return true;
        }
    }
}
