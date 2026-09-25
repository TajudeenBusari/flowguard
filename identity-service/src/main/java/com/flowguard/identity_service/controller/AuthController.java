package com.flowguard.identity_service.controller;

import com.flowguard.identity_service.dto.LoginRequest;
import com.flowguard.identity_service.service.AuthService;
import com.tjtechy.system.Result;
import com.tjtechy.system.StatusCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("${api.endpoint.base-url}/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthService authService;

  @PostMapping("/login")
  public Mono<Result> login(@Valid @RequestBody LoginRequest request){
    return authService.login(request)
            .map(loginResponse -> {
              return new Result("Login successful", true, loginResponse, StatusCode.SUCCESS);
            });
  }

  //only used for testing purposes, can be removed in production
//  @GetMapping("/test-me")
//  public Mono<Result> testMe(@AuthenticationPrincipal Jwt jwt){
//    CurrentUser currentUser = CurrentUser.fromJwt(jwt);
//
//    return Mono.just(new Result("Authenticated user info", true, currentUser, StatusCode.SUCCESS));
//  }
}
