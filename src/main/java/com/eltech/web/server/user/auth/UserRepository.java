package com.eltech.web.server.user.auth;

import com.eltech.web.server.user.ChatUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class UserRepository {
    private final List<ChatUser> users = new ArrayList<>();

    private final PasswordEncoder passwordEncoder;

    public UserRepository(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        this.users.add(new ChatUser("test-uid-1", "Chel", passwordEncoder.encode("1234")));
        this.users.add(new ChatUser("test-uid-2", "Bruh", passwordEncoder.encode("12345")));
    }

    public ChatUser getByUsername(String login) {
        return this.users.stream()
                .filter(user -> login.equals(user.getUsername()))
                .findFirst()
                .orElse(null);
    }

    public List<ChatUser> getAll() {
        return this.users;
    }

    // TODO: make transactional
    public boolean addUser(ChatUser user) {
        if (getByUsername(user.getUsername()) != null) {
            return false;
        }
        users.add(user);
        return true;
    }
}
