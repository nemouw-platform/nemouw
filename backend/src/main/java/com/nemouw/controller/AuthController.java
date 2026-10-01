package com.nemouw.controller;

import com.nemouw.http.ClientIp;
import com.nemouw.http.Json;
import com.nemouw.model.AuthDtos;
import com.nemouw.service.AuthService;

import static com.nemouw.http.JsonRoutes.*;
import static spark.Spark.path;
import static spark.Spark.post;

public class AuthController implements Controller {
    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @Override
    public void register() {
        path("/auth", () -> {
            post("/signup", created((req, res) ->
                    auth.signup(Json.read(req.body(), AuthDtos.SignupRequest.class))));

            post("/verify-otp", ok((req, res) ->
                    auth.verifyOtp(Json.read(req.body(), AuthDtos.VerifyOtpRequest.class), ClientIp.of(req), req.userAgent())));

            post("/resend-otp", accepted((req, res) -> {
                auth.resendOtp(Json.read(req.body(), AuthDtos.ResendOtpRequest.class));
                return null;
            }));

            post("/login", ok((req, res) ->
                    auth.login(Json.read(req.body(), AuthDtos.LoginRequest.class), ClientIp.of(req), req.userAgent())));
        });
    }
}