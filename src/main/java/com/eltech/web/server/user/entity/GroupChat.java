package com.eltech.web.server.user.entity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
public class GroupChat {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private long lastActivityTime;

    private String name;

    @ManyToOne
    @JoinColumn(name="creator_user_id", nullable=false)
    private ChatUser creator = null;

    @ManyToMany
    @JoinTable(
            name = "group_chat_users",
            joinColumns = { @JoinColumn(name = "chat_id") },
            inverseJoinColumns = { @JoinColumn(name = "user_id") }
    )
    private List<ChatUser> users = new ArrayList<>();

    public String inviteUid = null;


    public GroupChat() {

    }

    public GroupChat(String name, ChatUser creator) {
        this.name = name;
        this.creator = creator;
    }


    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ChatUser getCreator() {
        return creator;
    }

    public long getLastActivityTime() {
        return lastActivityTime;
    }

    public List<ChatUser> getUsers() {
        return users;
    }

    public String getInviteUid() {
        return inviteUid;
    }


    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCreator(ChatUser creator) {
        this.creator = creator;
    }

    public void setLastActivityTime(long lastActivityTime) {
        this.lastActivityTime = lastActivityTime;
    }

    public void setUsers(List<ChatUser> users) {
        this.users = users;
    }

    public void setInviteUid(String inviteUid) {
        this.inviteUid = inviteUid;
    }


    public void updateLastActivityTime() {
        setLastActivityTime(System.currentTimeMillis());
    }

    public String generateNewInviteId() {
        setInviteUid(UUID.randomUUID().toString());
        return getInviteUid();
    }

    public void removeInviteId() {
        setInviteUid(null);
    }


    public boolean hasUser(ChatUser user) {
        return getUsers().stream().anyMatch(usr -> Objects.equals(user.getId(), usr.getId()));
    }

    public boolean addUser(ChatUser user) {
        if (!hasUser(user)) {
            getUsers().add(user);
            return true;
        }
        return false;
    }

    public boolean removeUser(ChatUser user) {
        return getUsers().removeIf(usr -> Objects.equals(user.getId(), usr.getId()));
    }

    public boolean isCreator(ChatUser user) {
        return creator != null && Objects.equals(user.getId(), creator.getId());
    }

    public boolean canUserAccessInviteUid(ChatUser user) {
        return isCreator(user);
    }

    public boolean canUserRenameChat(ChatUser user) {
        return hasUser(user);
    }
}
