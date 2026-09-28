package com.varun.taskmgmtapi.security;
import com.varun.taskmgmtapi.models.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.util.Date;

//public interface JwtBuilder extends ClaimsMutator<JwtBuilder>
//Jwts class uses JWTBuilder interface


@Service
//@service annotation: "This class contains application/business logic.
// Please create an object of this class and manage it for me."
public class JwtService {
    //this class has one responsibility-- crete and verify JWT
    //It doesn't deal with HTTP requests.
    //It doesn't deal with users directly from the database.
    //It simply handles JWT operations.
    //That's good separation of responsibility.

//JWT or cryptography.
//main idea is : This constructor takes a secret string from application.properties,
// converts it into a cryptographic key, and stores that key so the application can create/verify JWT tokens.
    //so basically secretkey is a cryptographic key used to cretae/verify JWT
    private final SecretKey secretKey;
    private final Long expiration;
    public JwtService(
            //this part reads our secret
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") Long expiration
    ) {
        //secret is a string, but JWT cryptography library wants the secret in the form of bytes
        //we hav to import below libraries by ourselves
        //import io.jsonwebtoken.io.Decoders;
        //import io.jsonwebtoken.security.Keys;
        //it means "take this base-64 encoded string and convert it back to bytes(using decoder), we store these bytes in keyBytes array
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        //we will use byte array to create a cryptographic key (JWT compatible that JWT can use) as below:
            this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        //this creates the cryptographic key used by HS256.
        //HMAC = Hash-based Message Authentication Code
        //SHA= secure hash algorithm
        //secret key suitable for HMAC based JWT signing
        this.expiration = expiration;
    }
        public String generateToken(User user){
            Date now=new Date();
            Date expiryDate=new Date(now.getTime() + expiration);
//jwtsbuilder() for creating the signature
            return Jwts.builder()
                    .subject(user.getId().toString())
                    .claim("email",user.getEmail())
                    .claim("role",user.getRole().name())
                    .issuedAt(now)
                    .expiration(expiryDate)
                    //signWith throws InvalidKeyException error
                    .signWith(secretKey)
                    //compact() preduces final JWT string
                    .compact();
        }

        public Claims extractAllClaims(String token){
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        }

        public String extractUserId(String token){
            return extractAllClaims(token).getSubject();
        }

        public boolean isTokenValid(String token){
            try{
                extractAllClaims(token);
                return true;
            }
            catch(Exception e){
                return false;
            }
        }
    }



/*
"hello" -- normal text/string
computers represernt this as a byte array=[h-72,e-101,l-108,l-108,o-111]
BASE64 encoder-- converts this byte array into a encoded string
Original:
Hello

Base64:
SGVsbG8=
SGVsbG8= does not mean the secret has changed.

It is simply another representation of the same underlying bytes.

String secret = "SGVsbG8=";

byte[] keyBytes = Decoders.BASE64.decode(secret);

"SGVsbG8="
     ↓
Base64 decode
     ↓
[72, 101, 108, 108, 111]
It can be simply tested :

import java.util.Base64;
import java.util.Arrays;

public class Demo {

    public static void main(String[] args) {

        String secret = "SGVsbG8=";

        byte[] keyBytes = Base64.getDecoder().decode(secret);

        System.out.println(Arrays.toString(keyBytes));
    }
}


                 application.properties
                         │
                         ▼
                 jwt.secret
                         │
                         ▼
                  Base64 decode
                         │
                         ▼
                      byte[]
                         │
                         ▼
               hmacShaKeyFor()
                         │
                         ▼
                    SecretKey
                         │
                         ▼
                  generateToken()
                         │
                         ▼
                     JWT Token

                     and LATER------>
JWT Token
    │
    ▼
JwtService
    │
    ▼
secretKey
    │
    ▼
verify signature
    │
    ▼
Valid JWT / Invalid JWT

 */