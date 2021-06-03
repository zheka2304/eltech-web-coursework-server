package com.eltech.web.server.user.service;

import com.eltech.web.server.user.entity.ChatUser;
import com.eltech.web.server.user.entity.GroupChat;
import com.eltech.web.server.user.repo.GroupChatRepository;
import com.eltech.web.server.user.repo.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

@Service
public class UserService implements UserDetailsService {
    private final UserRepository repository;
    private final GroupChatRepository groupChatRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, GroupChatRepository groupChatRepository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.groupChatRepository = groupChatRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public ChatUser fetch(ChatUser user) {
        if (user == null) {
            return null;
        }
        return repository.findById(user.getId()).orElse(null);
    }

    public void save(ChatUser user) {
        if (user != null) {
            repository.save(user);
        }
    }

    public ChatUser saveAndFetch(ChatUser user) {
        save(user);
        return fetch(user);
    }

    public ChatUser getByUid(String uid) {
        return this.repository.findByUid(uid);
    }

    public ChatUser getByUsername(String login) {
        return this.repository.findByUsername(login);
    }

    @Transactional
    public ChatUser registerNewUser(String username, String password) {
        ChatUser user = new ChatUser(UUID.randomUUID().toString(), username, passwordEncoder.encode(password));

        if (repository.findByUsername(user.getUsername()) != null) {
            return null;
        }
        save(user);
        return getByUid(user.getUid());
    }

    @Transactional
    public boolean changeUsername(ChatUser user, String username) {
        ChatUser userByUsername = repository.findByUsername(username);
        if (userByUsername != null) {
            return false;
        }
        user.setUsername(username);
        save(user);
        return true;
    }

    @Transactional
    public void changePassword(ChatUser user, String password) {
        user.setPassword(passwordEncoder.encode(password));
        save(user);
    }

    @Transactional
    public void generateNewUid(ChatUser user) {
        user.setUid(UUID.randomUUID().toString());
        save(user);
    }

    @Transactional
    public boolean terminateUser(ChatUser user) {
        user = fetch(user);
        if (user != null) {
            for (GroupChat groupChat : new ArrayList<>(user.getGroupChats())) {
                groupChat.removeUser(user);
                groupChatRepository.save(groupChat);
            }
            repository.delete(user);
            return true;
        }
        return false;
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
