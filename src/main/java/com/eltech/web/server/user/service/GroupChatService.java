package com.eltech.web.server.user.service;

import com.eltech.web.server.user.entity.ChatUser;
import com.eltech.web.server.user.entity.GroupChat;
import com.eltech.web.server.user.repo.GroupChatRepository;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Objects;

@Service
public class GroupChatService {
    private final GroupChatRepository groupChatRepository;

    public GroupChatService(GroupChatRepository groupChatRepository) {
        this.groupChatRepository = groupChatRepository;
    }

    public GroupChat getById(long id) {
        return groupChatRepository.findById(id).orElse(null);
    }

    public GroupChat fetch(GroupChat chat) {
        return groupChatRepository.findById(chat.getId()).orElse(null);
    }

    public void save(GroupChat chat) {
        groupChatRepository.save(chat);
    }

    public GroupChat saveAndFetch(GroupChat chat) {
        save(chat);
        return fetch(chat);
    }

    @Transactional
    public GroupChat createGroupChat(ChatUser user, String chatName) {
        GroupChat groupChat = new GroupChat(chatName, user);
        groupChat.updateLastActivityTime();
        String inviteId = groupChat.generateNewInviteId();
        groupChat.getUsers().add(user);
        save(groupChat);
        return groupChatRepository.findGroupChatByInviteUid(inviteId);
    }

    @Transactional
    public GroupChat joinGroupChat(ChatUser user, String inviteUid) {
        GroupChat chat = groupChatRepository.findGroupChatByInviteUid(inviteUid);
        if (chat == null) {
            return null;
        }

        if (chat.addUser(user)) {
            chat.updateLastActivityTime();
            return saveAndFetch(chat);
        }
        return chat;
    }

    @Transactional
    public boolean leaveGroupChat(ChatUser user, GroupChat chat) {
        if (chat == null || user == null) {
            return false;
        }
        if (chat.removeUser(user)) {
            if (chat.getUsers().isEmpty()) {
                groupChatRepository.delete(chat);
            } else {
                chat.updateLastActivityTime();
                save(chat);
            }
            return true;
        }
        return false;
    }
}
