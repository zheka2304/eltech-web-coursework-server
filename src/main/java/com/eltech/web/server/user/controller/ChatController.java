package com.eltech.web.server.user.controller;

import com.eltech.web.server.user.entity.ChatUser;
import com.eltech.web.server.user.service.DialogService;
import com.eltech.web.server.user.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping(path = "/api/chat")
public class ChatController {
    private final UserService userService;
    private final DialogService dialogService;

    public ChatController(UserService userService, DialogService dialogService) {
        this.userService = userService;
        this.dialogService = dialogService;
    }

    @GetMapping(path = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object listChats(@AuthenticationPrincipal ChatUser user) {
        user = userService.fetch(user);
        return user.getDialogs();
    }

    @PostMapping(path = "/add_dialog", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object addDialog(@AuthenticationPrincipal ChatUser user, @RequestBody Map<String, String> payload) {
        String targetUid = payload.get("target");
        if (!StringUtils.hasLength(targetUid)) {
            return null;
        }
        return dialogService.getOrAddDialog(userService.fetch(user), targetUid);
    }

    @PostMapping(path = "/remove_dialog", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object removeDialog(@AuthenticationPrincipal ChatUser user, @RequestBody Map<String, String> payload) {
        String targetUid = payload.get("target");
        if (!StringUtils.hasLength(targetUid)) {
            return Collections.singletonMap("success", false);
        }
        return Collections.singletonMap("success", dialogService.removeDialog(userService.fetch(user), targetUid));
    }
}
