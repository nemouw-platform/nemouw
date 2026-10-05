package com.nemouw.controller;

import com.nemouw.http.AuthCookies;
import com.nemouw.http.ClientIp;
import com.nemouw.http.Json;
import com.nemouw.model.AuthDtos;
import com.nemouw.model.ClientInfo;
import com.nemouw.service.AuthService;
import spark.Request;
import spark.Response;

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
                    startSession(res, auth.verifyOtp(Json.read(req.body(), AuthDtos.VerifyOtpRequest.class), client(req)))));

            post("/resend-otp", accepted((req, res) -> {
                auth.resendOtp(Json.read(req.body(), AuthDtos.ResendOtpRequest.class));
                return null;
            }));

            post("/login", ok((req, res) ->
                    startSession(res, auth.login(Json.read(req.body(), AuthDtos.LoginRequest.class), client(req)))));

            post("/refresh", ok((req, res) ->
                    startSession(res, auth.refresh(req.cookie(AuthCookies.REFRESH)))));

            post("/logout", noContent((req, res) -> {
                auth.logout(req.cookie(AuthCookies.REFRESH));
                AuthCookies.clear(res);      // cleared even if the cookie was missing or the token unknown
                return null;
            }));
        });
    }

    private static AuthDtos.SessionResponse startSession(Response res, AuthDtos.IssuedSession s) {
        AuthCookies.set(res, s);
        return AuthDtos.SessionResponse.from(s);
    }

    private ClientInfo client(Request req) {
        return ClientInfo.from(ClientIp.resolve(req), req.userAgent());
    }
}