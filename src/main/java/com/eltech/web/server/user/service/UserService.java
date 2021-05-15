package com.eltech.web.server.user.service;

import com.eltech.web.server.user.entity.ChatUser;
import com.eltech.web.server.user.repo.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService implements UserDetailsService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public ChatUser fetch(ChatUser user) {
        return repository.findById(user.getId()).orElse(null);
    }

    public void save(ChatUser user) {
        repository.save(user);
    }

    public ChatUser saveAndFetch(ChatUser user) {
        save(user);
        return fetch(user);
    }

    public ChatUser getByUsername(String login) {
        return this.repository.findByUsername(login);
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
