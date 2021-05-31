package com.eltech.web.server.user.controller;

import com.eltech.web.server.user.entity.ChatUser;
import com.eltech.web.server.user.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.Collections;

import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

@RestController
@RequestMapping(path="/api/auth")
public class UserAuthController {
    private final UserService userService;
    private final AuthenticationManager authManager;
    private final PasswordEncoder passwordEncoder;

    public UserAuthController(UserService userService, AuthenticationManager authManager, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.authManager = authManager;
        this.passwordEncoder = passwordEncoder;
    }

    private static class UserCredentials {
        public String username;
        public String password;

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public boolean isDataValid() {
            return StringUtils.hasLength(username) && StringUtils.hasLength(password);
        }
    }

    private static class LoginOrLogoutResult {
        public final boolean success;
        public final String error;

        private LoginOrLogoutResult(boolean success, String error) {
            this.success = success;
            this.error = error;
        }
    }

    private void logInUser(HttpServletRequest request, HttpServletResponse response, UsernamePasswordAuthenticationToken authReq) {
        Authentication auth = authManager.authenticate(authReq);
        SecurityContext sc = SecurityContextHolder.getContext();
        sc.setAuthentication(auth);
        HttpSession session = request.getSession(true);
        session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, sc);
    }

    @PostMapping(path = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody Object login(@AuthenticationPrincipal ChatUser loggedInUser, @RequestBody UserCredentials credentials, HttpServletRequest request, HttpServletResponse response) {
        if (credentials.isDataValid()) {
            if (loggedInUser != null) {
                return new LoginOrLogoutResult(false, "Вы уже зашли в аккаунт");
            }

            ChatUser user = userService.getByUsername(credentials.getUsername());
            if (user == null || !passwordEncoder.matches(credentials.getPassword(), user.getPassword())) {
                return new LoginOrLogoutResult(false, "Неправильное имя пользователя или пароль");
            }

            UsernamePasswordAuthenticationToken authReq = new UsernamePasswordAuthenticationToken(user, credentials.getPassword());
            logInUser(request, response, authReq);
            return new LoginOrLogoutResult(true, null);
        }
        return new LoginOrLogoutResult(false, "non-empty strings must be provided both for username and password");
    }

    @PostMapping(path = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody Object logout(@AuthenticationPrincipal ChatUser user, HttpServletRequest request) {
        if (user == null) {
            return new LoginOrLogoutResult(false, "Вы не зашли в аккаунт");
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null){
            new SecurityContextLogoutHandler().logout(request, null, auth);
        }
        SecurityContextHolder.getContext().setAuthentication(null);
        return new LoginOrLogoutResult(true, null);
    }

    @PostMapping(path = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody Object logout(@AuthenticationPrincipal ChatUser loggedInUser, @RequestBody UserCredentials credentials, HttpServletRequest request, HttpServletResponse response) {
        if (credentials.isDataValid()) {
            if (loggedInUser != null) {
                return new LoginOrLogoutResult(false, "Вы уже зашли в аккаунт");
            }

            ChatUser user = userService.getByUsername(credentials.getUsername());
            if (user != null) {
                return new LoginOrLogoutResult(false, "Имя пользователя уже занято");
            }

            user = userService.registerNewUser(credentials.getUsername(), credentials.getPassword());
            UsernamePasswordAuthenticationToken authReq = new UsernamePasswordAuthenticationToken(user, credentials.getPassword());
            logInUser(request, response, authReq);
            return new LoginOrLogoutResult(true, null);
        }
        return new LoginOrLogoutResult(false, "non-empty strings must be provided both for username and password");
    }

    @GetMapping(path = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody Object getCurrentUser(@AuthenticationPrincipal ChatUser user, HttpServletRequest request) {
        return Collections.singletonMap("user", userService.fetch(user));
    }
}
