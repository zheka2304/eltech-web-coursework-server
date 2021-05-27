package com.eltech.web.server.user.controller;


import com.eltech.web.server.user.entity.ChatUser;
import com.eltech.web.server.user.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping(path="/api/user")
public class UserInfoController {
    private final UserService userService;

    public UserInfoController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping(path = "/get", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody Map<String, Object> getCurrentUser(@RequestParam String uid) {
        return Collections.singletonMap("user", userService.getByUid(uid));
    }
}
