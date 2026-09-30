package com.nemouw.controller;

import com.nemouw.http.Json;
import com.nemouw.http.route.Routes;
import com.nemouw.model.AuthDtos.VerifyOtpRequest;
import com.nemouw.model.AuthDtos.SignupRequest;
import com.nemouw.service.AuthService;
import spark.Request;

import static spark.Spark.path;
import static spark.Spark.post;

public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    public void register() {
        path("/v1/auth", () -> {
            post("/signup", Routes.json(201, (req, res) ->
                    auth.signup(Json.read(req.body(), SignupRequest.class))));

            post("/signup/verify-otp", Routes.json(200, (req, res) ->
                    auth.verifyOtp(Json.read(req.body(), VerifyOtpRequest.class), clientIp(req), req.userAgent())));
        });
    }

    private static String clientIp(Request req) {
        String ip = req.ip();
        int zone = ip == null ? -1 : ip.indexOf('%');
        return zone >= 0 ? ip.substring(0, zone) : ip;
    }
}