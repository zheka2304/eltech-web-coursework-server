package com.eltech.web.server.user.auth;

import com.eltech.web.server.user.ChatUser;
import org.springframework.data.repository.CrudRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

@Repository
public interface UserRepository extends CrudRepository<ChatUser, Long> {
    ChatUser findByUsername(String username);

    @Transactional
    default boolean addUser(ChatUser user) {
        if (findByUsername(user.getUsername()) != null) {
            return false;
        }
        save(user);

        return true;
    }
}
