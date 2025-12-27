package com.example.broilerfarm.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT Service - Domain Level Service
 *
 * Responsabilități:
 * - Generare token-uri JWT pentru utilizatori autentificați
 * - Validare token-uri JWT
 * - Extragere informații din token-uri (username, roles, expiry date)
 *
 * Logica din spate:
 * JWT-ul este format din 3 părți separate prin punct:
 * 1. Header - conține tipul token-ului și algoritmul de semnare
 * 2. Payload - conține claims (date despre user: username, roles, expiry)
 * 3. Signature - semnătură criptografică care garantează integritatea
 *
 * Formula: Base64(Header).Base64(Payload).Signature
 * Exemplu: eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huQGV4YW1wbGUuY29tIn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
 */
@Service
public class JwtService {

    /**
     * Cheia secretă folosită pentru semnarea token-urilor
     * IMPORTANT: În producție, această cheie trebuie păstrată în variabile de mediu sau vault
     * Lungime minimă recomandată: 256 bits (32 caractere pentru HS256)
     */
    @Value("${jwt.secret.key:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String secretKey;

    /**
     * Durata de valabilitate a token-ului în milisecunde
     * Default: 24 ore (86400000 ms)
     */
    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    /**
     * Generează un token JWT pentru un utilizator
     *
     * @param username - email-ul utilizatorului (folosit ca subject în JWT)
     * @return token JWT ca String
     *
     * Procesul:
     * 1. Creează un map de claims (date suplimentare - pot include roles, permissions)
     * 2. Setează subject-ul (username/email)
     * 3. Setează data creării (issued at)
     * 4. Setează data expirării (current time + jwtExpiration)
     * 5. Semnează token-ul cu cheia secretă folosind HS256
     */
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username);
    }

    /**
     * Generează token cu claims custom (de ex. roles, permissions)
     *
     * @param extraClaims - informații suplimentare de adăugat în token (roles, etc.)
     * @param username - email-ul utilizatorului
     * @return token JWT
     */
    public String generateToken(Map<String, Object> extraClaims, String username) {
        return createToken(extraClaims, username);
    }

    /**
     * Creează efectiv token-ul JWT
     *
     * @param claims - date suplimentare în token
     * @param subject - username/email (identificatorul principal al userului)
     * @return token JWT semnat
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)                                    // Claims custom
                .setSubject(subject)                                   // Username/email
                .setIssuedAt(new Date(System.currentTimeMillis()))    // Când a fost creat
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration)) // Când expiră
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)   // Semnare cu HS256
                .compact();                                            // Generare string final
    }

    /**
     * Validează un token JWT
     *
     * @param token - token-ul de validat
     * @param username - username-ul userului curent
     * @return true dacă token-ul e valid, false altfel
     *
     * Verificări:
     * 1. Username-ul din token corespunde cu cel furnizat
     * 2. Token-ul nu a expirat
     */
    public boolean isTokenValid(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }

    /**
     * Extrage username-ul (subject) din token
     *
     * @param token - token JWT
     * @return username/email din token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrage data de expirare din token
     *
     * @param token - token JWT
     * @return data când expiră token-ul
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extrage un claim specific din token folosind o funcție de rezolvare
     *
     * @param token - token JWT
     * @param claimsResolver - funcție care specifică ce claim vrei să extragi
     * @return valoarea claim-ului
     *
     * Exemplu: extractClaim(token, Claims::getSubject) - extrage subject-ul
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extrage toate claims-urile din token
     *
     * @param token - token JWT
     * @return obiect Claims cu toate datele din token
     *
     * Procesul:
     * 1. Parsează token-ul
     * 2. Verifică semnătura folosind cheia secretă
     * 3. Returnează payload-ul (claims)
     *
     * Dacă token-ul e invalid sau expirat, aruncă excepție
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) getSignInKey())  // în loc de setSigningKey
                .build()
                .parseSignedClaims(token)  // în loc de parseClaimsJws
                .getPayload();  // în loc de getBody
    }

    /**
     * Verifică dacă token-ul a expirat
     *
     * @param token - token JWT
     * @return true dacă token-ul a expirat, false altfel
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Convertește cheia secretă din String în Key object pentru semnare
     *
     * @return Key object folosit pentru semnarea JWT-urilor
     *
     * Procesul:
     * 1. Decodează cheia din Base64
     * 2. Creează un Key object HMAC pentru algoritmul HS256
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Extrage rolurile din token (dacă au fost adăugate la generare)
     *
     * @param token - token JWT
     * @return lista de roluri sau null
     */
    @SuppressWarnings("unchecked")
    public java.util.List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("roles", java.util.List.class);
    }
}