package com.eltech.web.server.user.controller;

import com.eltech.web.server.user.entity.ChatUser;
import com.eltech.web.server.user.entity.Dialog;
import com.eltech.web.server.user.entity.GroupChat;
import com.eltech.web.server.user.service.DialogService;
import com.eltech.web.server.user.service.GroupChatService;
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
    private final GroupChatService groupChatService;

    public ChatController(UserService userService, DialogService dialogService, GroupChatService groupChatService) {
        this.userService = userService;
        this.dialogService = dialogService;
        this.groupChatService = groupChatService;
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

    private class UniversalChatWrap {
        private final String chatId;
        private final ChatType chatType;
        private final String chatTitle;
        private final long lastActivityTime;
        private final List<String> targets;

        public UniversalChatWrap(ChatUser user, Dialog dialog) {
            chatId = ChatType.DIALOG.idPrefix + dialog.getId();
            chatType = ChatType.DIALOG;
            targets = Collections.singletonList(dialog.getTarget());
            lastActivityTime = dialog.getLastActivityTime();

            String targetUid = dialog.getTarget();
            ChatUser targetUser = userService.getByUid(targetUid);
            chatTitle = targetUser != null ? targetUser.getUsername() : targetUid;
        }

        public UniversalChatWrap(ChatUser user, GroupChat groupChat) {
            chatId = ChatType.GROUP_CHAT.idPrefix + groupChat.getId();
            chatType = ChatType.GROUP_CHAT;
            chatTitle = groupChat.getName();
            targets = groupChat.getUsers().stream()
                    .filter(usr -> !usr.getId().equals(user.getId()))
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

        public String getChatTitle() {
            return chatTitle;
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
    public UniversalChatWrap getChatById(@AuthenticationPrincipal ChatUser user, @RequestParam(name="chatId") String parChatId) {
        ChatId chatId = new ChatId(parChatId);
        if (!chatId.isValid()) {
            return null;
        }

        user = userService.fetch(user);
        switch (chatId.getType()) {
            case DIALOG -> {
                Dialog dialog = user.getDialog(chatId.getId());
                return dialog != null ? new UniversalChatWrap(user, dialog) : null;
            }
            case GROUP_CHAT -> {
                GroupChat groupChat = user.getGroupChat(chatId.getId());
                return groupChat != null ? new UniversalChatWrap(user, groupChat) : null;
            }
        }

        return null;
    }


    private class ChatMemberInfo {
        public final String targetUid;
        public final ChatUser chatUser;
        public final boolean isCreator;

        public ChatMemberInfo(String targetUid, boolean isCreator) {
            this.targetUid = targetUid;
            this.chatUser = userService.getByUid(targetUid);
            this.isCreator = isCreator;
        }

        public ChatMemberInfo(ChatUser user, boolean isCreator) {
            this.targetUid = user.getUid();
            this.chatUser = user;
            this.isCreator = isCreator;
        }
    }

    @GetMapping(path = "/get_members", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ChatMemberInfo> getChatMembers(@AuthenticationPrincipal ChatUser user, @RequestParam(name="chatId") String parChatId) {
        ChatId chatId = new ChatId(parChatId);
        if (!chatId.isValid()) {
            return Collections.emptyList();
        }

        user = userService.fetch(user);
        switch (chatId.getType()) {
            case DIALOG -> {
                Dialog dialog = user.getDialog(chatId.getId());
                if (dialog != null) {
                    List<ChatMemberInfo> result = new ArrayList<>();
                    result.add(new ChatMemberInfo(dialog.getUser(), false));
                    result.add(new ChatMemberInfo(dialog.getTarget(), false));
                    return result;
                }
                return Collections.emptyList();
            }
            case GROUP_CHAT -> {
                GroupChat groupChat = user.getGroupChat(chatId.getId());
                if (groupChat != null) {
                    return groupChat.getUsers().stream().map(usr -> new ChatMemberInfo(usr, groupChat.isCreator(usr))).collect(Collectors.toList());
                }
                return Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }

    @PostMapping(path = "/update_last_activity", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> updateLastActivity(@AuthenticationPrincipal ChatUser user, @RequestBody Map<String, String> payload) {
        ChatId chatId = new ChatId(payload.get("chatId"));
        if (!chatId.isValid()) {
            return Collections.singletonMap("success", false);
        }

        user = userService.fetch(user);
        boolean success = false;

        switch (chatId.getType()) {
            case DIALOG -> {
                Dialog dialog = user.getDialog(chatId.getId());
                if (dialog != null) {
                    dialog.updateLastActivityTime();
                    success = true;
                }
            }
            case GROUP_CHAT -> {
                GroupChat groupChat = user.getGroupChat(chatId.getId());
                if (groupChat != null) {
                    groupChat.updateLastActivityTime();
                    success = true;
                }
            }
        }

        return Collections.singletonMap("success", success);
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

    @PostMapping(path = "/create_group", produces = MediaType.APPLICATION_JSON_VALUE)
    public UniversalChatWrap createGroupChat(@AuthenticationPrincipal ChatUser user, @RequestBody Map<String, String> payload) {
        String chatName = payload.get("chatName");
        if (!StringUtils.hasLength(chatName)) {
            return null;
        }

        user = userService.fetch(user);
        return new UniversalChatWrap(user, groupChatService.createGroupChat(user, chatName));
    }

    @PostMapping(path = "/join_group", produces = MediaType.APPLICATION_JSON_VALUE)
    public UniversalChatWrap joinGroupChat(@AuthenticationPrincipal ChatUser user, @RequestBody Map<String, String> payload) {
        String inviteUid = payload.get("inviteUid");
        if (!StringUtils.hasLength(inviteUid)) {
            return null;
        }

        user = userService.fetch(user);
        GroupChat chat = groupChatService.joinGroupChat(user, inviteUid);
        return chat != null ? new UniversalChatWrap(user, chat) : null;
    }

    private GroupChat getGroupChatByUserAndCheck(ChatUser user, ChatId chatId) {
        if (!chatId.isValid() || chatId.getType() != ChatType.GROUP_CHAT) {
            return null;
        }

        GroupChat chat = groupChatService.getById(chatId.getId());
        if (chat == null) {
            return null;
        }

        if (!chat.hasUser(user)) {
            return null;
        }

        return chat;
    }

    @GetMapping(path = "/get_invite_uid", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getGroupChatInviteUid(@AuthenticationPrincipal ChatUser user, @RequestParam String chatId) {
        user = userService.fetch(user);
        GroupChat chat = getGroupChatByUserAndCheck(user, new ChatId(chatId));

        if (chat == null || !chat.canUserAccessInviteUid(user)) {
            return Collections.singletonMap("inviteUid", null);
        }
        return Collections.singletonMap("inviteUid", chat.getInviteUid());
    }

    @PostMapping(path = "/revoke_invite_uid", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> revokeGroupChatInviteUid(@AuthenticationPrincipal ChatUser user, @RequestParam String chatId) {
        user = userService.fetch(user);
        GroupChat chat = getGroupChatByUserAndCheck(user, new ChatId(chatId));

        if (chat == null || !chat.canUserAccessInviteUid(user)) {
            return Collections.singletonMap("inviteUid", null);
        }

        String newInviteUid = chat.generateNewInviteId();
        groupChatService.save(chat);
        return Collections.singletonMap("inviteUid", newInviteUid);
    }

    @PostMapping(path = "/rename_group", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> renameGroupChat(@AuthenticationPrincipal ChatUser user, @RequestBody Map<String, String> payload) {
        ChatId chatId = new ChatId(payload.get("chatId"));
        String newChatName = payload.get("newChatName");
        if (!StringUtils.hasLength(newChatName)) {
            return Collections.singletonMap("success", false);
        }

        user = userService.fetch(user);
        GroupChat chat = getGroupChatByUserAndCheck(user, chatId);
        if (chat == null || !chat.canUserRenameChat(user)) {
            return Collections.singletonMap("success", false);
        }

        chat.setName(newChatName);
        groupChatService.save(chat);
        return Collections.singletonMap("success", true);
    }

    @PostMapping(path = "/leave", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> leaveChat(@AuthenticationPrincipal ChatUser user, @RequestBody Map<String, String> payload) {
        ChatId chatId = new ChatId(payload.get("chatId"));
        if (!chatId.isValid()) {
            return Collections.singletonMap("success", false);
        }

        user = userService.fetch(user);
        switch (chatId.getType()) {
            case DIALOG -> {
                Dialog dialog = user.getDialog(chatId.getId());
                if (dialog != null) {
                    return Collections.singletonMap("success", dialogService.removeDialog(user, dialog));
                }
            }
            case GROUP_CHAT -> {
                GroupChat groupChat = user.getGroupChat(chatId.getId());
                if (groupChat != null) {
                    return Collections.singletonMap("success", groupChatService.leaveGroupChat(user, groupChat));
                }
            }
        }

        return Collections.singletonMap("success", false);
    }
}
