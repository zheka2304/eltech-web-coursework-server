package com.eltech.web.server.user.controller;

import com.eltech.web.server.user.entity.ChatUser;
import com.eltech.web.server.user.entity.Dialog;
import com.eltech.web.server.user.entity.GroupChat;
import com.eltech.web.server.user.service.DialogService;
import com.eltech.web.server.user.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping(path = "/api/chat")
public class ChatController {
    private final UserService userService;
    private final DialogService dialogService;

    public ChatController(UserService userService, DialogService dialogService) {
        this.userService = userService;
        this.dialogService = dialogService;
    }


    private enum ChatType {
        DIALOG("d"),
        GROUP_CHAT("g");

        public final String idPrefix;

        ChatType(String idPrefix) {
            this.idPrefix = idPrefix;
        }
    }

    public static class ChatId {
        public final ChatType type;
        public final long id;

        public ChatId(String id) {
            if (id == null) {
                this.type = null;
                this.id = 0;
                return;
            }

            for (ChatType type : ChatType.values()) {
                if (id.startsWith(type.idPrefix)) {
                    long _id;
                    try {
                        _id = Long.parseLong(id.substring(type.idPrefix.length()));
                    } catch (NumberFormatException e) {
                        break;
                    }
                    this.id = _id;
                    this.type = type;
                    return;
                }
            }

            this.type = null;
            this.id = 0;
        }

        public ChatType getType() {
            return type;
        }

        public long getId() {
            return id;
        }

        public String getStringId() {
            return type != null ? type.idPrefix + id : null;
        }

        public boolean isValid() {
            return type != null;
        }
    }

    private static class UniversalChatWrap {
        private final String chatId;
        private final ChatType chatType;
        private final long lastActivityTime;
        private final List<String> targets;

        public UniversalChatWrap(ChatUser user, Dialog dialog) {
            chatId = ChatType.DIALOG.idPrefix + dialog.getId();
            chatType = ChatType.DIALOG;
            targets = Collections.singletonList(dialog.getTarget());
            lastActivityTime = dialog.getLastActivityTime();
        }

        public UniversalChatWrap(ChatUser user, GroupChat groupChat) {
            chatId = ChatType.GROUP_CHAT.idPrefix + groupChat.getId();
            chatType = ChatType.GROUP_CHAT;
            targets = groupChat.getUsers().stream()
                    .filter(usr -> usr.getId().equals(user.getId()))
                    .map(ChatUser::getUid)
                    .collect(Collectors.toList());
            lastActivityTime = groupChat.getLastActivityTime();
        }

        public String getChatId() {
            return chatId;
        }

        public ChatType getChatType() {
            return chatType;
        }

        public List<String> getTargets() {
            return targets;
        }

        public long getLastActivityTime() {
            return lastActivityTime;
        }
    }


    @GetMapping(path = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<UniversalChatWrap> listChats(@AuthenticationPrincipal ChatUser _user) {
        ChatUser user = userService.fetch(_user);
        return Stream.concat(
                user.getDialogs().stream().map(dialog -> new UniversalChatWrap(user, dialog)),
                user.getGroupChats().stream().map(groupChat -> new UniversalChatWrap(user, groupChat))
        ).sorted(Comparator.comparingLong(chat -> -chat.getLastActivityTime())).collect(Collectors.toList());
    }

    @GetMapping(path = "/get", produces = MediaType.APPLICATION_JSON_VALUE)
    public UniversalChatWrap getChatById(@AuthenticationPrincipal ChatUser user, @RequestBody Map<String, String> payload) {
        ChatId chatId = new ChatId(payload.get("chatId"));
        if (!chatId.isValid()) {
            return null;
        }

        user = userService.fetch(user);
        switch (chatId.getType()) {
            case DIALOG -> {
                for (Dialog dialog : user.getDialogs()) {
                    if (Objects.equals(chatId.id, dialog.getId())) {
                        return new UniversalChatWrap(user, dialog);
                    }
                }
                return null;
            }
            case GROUP_CHAT -> {
                for (GroupChat groupChat : user.getGroupChats()) {
                    if (Objects.equals(chatId.id, groupChat.getId())) {
                        return new UniversalChatWrap(user, groupChat);
                    }
                }
                return null;
            }
        }

        return null;
    }

    @GetMapping(path = "/get_members", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> getChatMemebers(@AuthenticationPrincipal ChatUser user, @RequestBody Map<String, String> payload) {
        ChatId chatId = new ChatId(payload.get("chatId"));
        if (!chatId.isValid()) {
            return Collections.emptyList();
        }

        user = userService.fetch(user);
        switch (chatId.getType()) {
            case DIALOG -> {
                for (Dialog dialog : user.getDialogs()) {
                    if (Objects.equals(chatId.id, dialog.getId())) {
                        List<String> result = new ArrayList<>();
                        result.add(dialog.getUser().getUid());
                        result.add(dialog.getTarget());
                        return result;
                    }
                }
                return Collections.emptyList();
            }
            case GROUP_CHAT -> {
                for (GroupChat groupChat : user.getGroupChats()) {
                    if (Objects.equals(chatId.id, groupChat.getId())) {
                        return groupChat.getUsers().stream().map(ChatUser::getUid).collect(Collectors.toList());
                    }
                }
                return Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }

    @PostMapping(path = "/add_dialog", produces = MediaType.APPLICATION_JSON_VALUE)
    public UniversalChatWrap addDialog(@AuthenticationPrincipal ChatUser user, @RequestBody Map<String, String> payload) {
        String targetUid = payload.get("target");
        if (!StringUtils.hasLength(targetUid)) {
            return null;
        }
        user = userService.fetch(user);
        return new UniversalChatWrap(user, dialogService.getOrAddDialog(user, targetUid));
    }

    @PostMapping(path = "/remove_dialog", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> removeDialog(@AuthenticationPrincipal ChatUser user, @RequestBody Map<String, String> payload) {
        String targetUid = payload.get("target");
        if (!StringUtils.hasLength(targetUid)) {
            return Collections.singletonMap("success", false);
        }
        return Collections.singletonMap("success", dialogService.removeDialog(userService.fetch(user), targetUid));
    }
}
