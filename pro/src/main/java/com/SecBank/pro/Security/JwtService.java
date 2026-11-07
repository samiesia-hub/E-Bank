package com.SecBank.pro.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {
    @Value("${app.jwt.secret")
    private String secret;
    @Value("${app.jwt.expiration}")
    private Long expiration;
    private SecretKey getSigningKey(){
        byte[] keybytes= Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keybytes);
     }
     public String generateToken(UserDetails userDetails){
         Date now =new Date();
         Date expireDate=new Date(now.getTime()+expiration);
         return Jwts.builder()
                 .subject(userDetails.getUsername())
                 .issuedAt(now)
                 .expiration(expireDate)
                 .signWith(getSigningKey())
                 .compact();
     }
     private <T> T extractClaim(String token, Function<Claims,T> claimsResolver){
        Claims claims=Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
     }
    //username extraction
     public String extractUsername(String token){
        return extractClaim(token,Claims::getSubject);
     }
     private  boolean isTokenExpired(String token){
        return extractClaim(token,Claims::getExpiration).before(new Date());

     }
    public boolean isTokenValid(String token,UserDetails userDetails){
        String username=extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);

     }
}
