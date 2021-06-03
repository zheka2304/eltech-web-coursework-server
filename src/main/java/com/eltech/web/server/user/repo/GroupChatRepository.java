package com.eltech.web.server.user.repo;

import com.eltech.web.server.user.entity.GroupChat;
import org.springframework.data.repository.CrudRepository;

public interface GroupChatRepository extends CrudRepository<GroupChat, Long> {
    GroupChat findGroupChatByInviteUid(String inviteUid);
}
