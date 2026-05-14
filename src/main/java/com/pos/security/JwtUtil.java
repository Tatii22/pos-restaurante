package com.pos.security;



@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationTimeMs;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secretKeyBase64,
            @Value("${app.jwt.expiration-ms:36000000}") long expirationTimeMs
    ) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKeyBase64));
        this.expirationTimeMs = expirationTimeMs;
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTimeMs))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

