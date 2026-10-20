package com.nemouw.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    // This creates a GET route at http://localhost:8080/test
    @GetMapping("/test")
    public String helloRoute() {
        return "Hello from the Dockerized backend!";
    }
}
