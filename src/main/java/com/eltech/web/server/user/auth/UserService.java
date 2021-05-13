package com.eltech.web.server.user.auth;

import com.eltech.web.server.user.ChatUser;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserService implements UserDetailsService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<ChatUser> getAll() {
        return this.repository.getAll();
    }

    public ChatUser getByUsername(String login) {
        return this.repository.getByUsername(login);
    }

    public ChatUser registerNewUser(String username, String password) {
        ChatUser user = new ChatUser(UUID.randomUUID().toString(), username, passwordEncoder.encode(password));
        return repository.addUser(user) ? user : null;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        ChatUser user = getByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("user not found: " + username);
        }
        return user;
    }
}
