package com.eltech.web.server.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

@Entity
public class ChatUser implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String uid;
    private String username;
    private String password;

    @OneToMany(mappedBy = "user", orphanRemoval = true, cascade = { CascadeType.ALL }, fetch = FetchType.EAGER)
    private List<Dialog> dialogs = new ArrayList<>();

    @ManyToMany(cascade = { CascadeType.ALL })
    @JoinTable(
            name = "group_chat_users",
            joinColumns = { @JoinColumn(name = "user_id") },
            inverseJoinColumns = { @JoinColumn(name = "chat_id") }
    )
    private List<GroupChat> groupChats = new ArrayList<>();

    public ChatUser() {

    }

    public ChatUser(String uid, String username, String password) {
        this.uid = uid;
        this.username = username;
        this.password = password;
    }


    public Long getId() {
        return id;
    }

    public String getUid() {
        return uid;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return password;
    }

    @JsonIgnore
    public List<Dialog> getDialogs() {
        return dialogs;
    }

    @JsonIgnore
    public List<GroupChat> getGroupChats() {
        return groupChats;
    }


    public void setId(Long id) {
        this.id = id;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setDialogs(List<Dialog> dialogs) {
        this.dialogs = dialogs;
    }

    public void setGroupChats(List<GroupChat> groupChats) {
        this.groupChats = groupChats;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return true;
    }

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return new HashSet<>();
    }


    public Dialog getDialog(long id) {
        for (Dialog dialog : getDialogs()) {
            if (dialog.getId() == id) {
                return dialog;
            }
        }
        return null;
    }

    public GroupChat getGroupChat(long id) {
        for (GroupChat groupChat : getGroupChats()) {
            if (groupChat.getId() == id) {
                return groupChat;
            }
        }
        return null;
    }
}
