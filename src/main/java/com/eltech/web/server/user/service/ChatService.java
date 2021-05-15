package com.eltech.web.server.user.service;

import com.eltech.web.server.user.entity.GroupChat;
import com.eltech.web.server.user.entity.ChatUser;
import com.eltech.web.server.user.repo.ChatRepository;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
public class ChatService {
    private final ChatRepository repository;

    public ChatService(ChatRepository repository) {
        this.repository = repository;
    }
}
