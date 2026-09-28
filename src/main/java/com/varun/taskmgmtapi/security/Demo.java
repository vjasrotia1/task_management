package com.varun.taskmgmtapi.security;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Arrays;

public class Demo {

        public static void main(String[] args) {
            String s="Hello";
            //encode it first using base 64 encoder
            String initialString= Base64.getEncoder().encodeToString(s.getBytes());
            System.out.println("encoded string from SecretKey obj is "+ initialString);

            //decode same string using base 64 decoder
            byte[] keyBytes = Base64.getDecoder().decode(initialString);

            //System.out.println(Arrays.toString(keyBytes));

            SecretKey sk= Keys.hmacShaKeyFor(keyBytes);

            //it means extract the raw bytes from SecretKey object
            byte[] rawbytes=sk.getEncoded();
            //System.out.println(Arrays.toString(rawbytes));

            // Encode it back to Base64
            String encodedString= Encoders.BASE64.encode(rawbytes);

            System.out.println("encoded string from SecretKey obj is "+ encodedString);


        }
    }

