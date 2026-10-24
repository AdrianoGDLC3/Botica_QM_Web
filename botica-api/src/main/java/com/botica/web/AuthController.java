package com.botica.web;

import com.botica.dto.Dtos.*;
import com.botica.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;

    @PostMapping("/auth/login")
    public LoginRes login(@Valid @RequestBody LoginReq r) { return auth.login(r); }
}
