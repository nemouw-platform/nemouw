package com.nemouw.user.identity;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
class PasswordConfig {

    @Bean
    PasswordEncoder passwordEncoder(PasswordProperties props) {
        PasswordProperties.Argon2 a = props.argon2();
        return new Argon2PasswordEncoder(a.saltLength(), a.hashLength(),
                a.parallelism(), a.memoryKb(), a.iterations());
    }
}