package com.app.english.controller;

import com.app.english.dto.LoginRequest;
import com.app.english.dto.RegisterRequest;
import com.app.english.models.User;
import com.app.english.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }
}

