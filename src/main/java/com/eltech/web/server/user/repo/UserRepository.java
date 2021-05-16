package com.eltech.web.server.user.repo;

import com.eltech.web.server.user.entity.ChatUser;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

@Repository
public interface UserRepository extends CrudRepository<ChatUser, Long> {
    ChatUser findByUsername(String username);

    ChatUser findByUid(String uid);
}
