package com.nemouw.controller;

import com.nemouw.http.Json;
import com.nemouw.http.route.Routes;
import com.nemouw.model.AuthDtos.SignupRequest;
import com.nemouw.service.AuthService;

import static spark.Spark.path;
import static spark.Spark.post;

public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    public void register() {
        path("/v1/auth", () ->
                post("/signup", Routes.json(201, (req, res) ->
                        auth.signup(Json.read(req.body(), SignupRequest.class)))));
    }
}