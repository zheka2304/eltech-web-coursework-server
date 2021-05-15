package com.eltech.web.server.user.repo;

import com.eltech.web.server.user.entity.GroupChat;
import com.eltech.web.server.user.entity.ChatUser;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ChatRepository extends CrudRepository<GroupChat, Long> {
    List<GroupChat> findChatByUsersContaining(ChatUser user);
}
