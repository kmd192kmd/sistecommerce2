package com.example.java;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

public class AdminPasswordTest {
    @Test
    public void generatePassword() {
        Argon2PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        String rawPassword = "admin";
        String encodedPassword = encoder.encode(rawPassword);
        System.out.println("==================================================");
        System.out.println("RAW: " + rawPassword);
        System.out.println("HASH: " + encodedPassword);
        System.out.println("==================================================");
    }
}
