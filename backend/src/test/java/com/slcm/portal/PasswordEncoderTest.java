
package com.slcm.portal;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordEncoderTest {

    @Test
    void generatePasswordHash() {
        var encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("password");

        System.out.println("Generated hash: " + hash);
        System.out.println("Matches password: "
                + encoder.matches("password", hash));
    }
}

