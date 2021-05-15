package com.eltech.web.server.user.entity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class GroupChat {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToMany
    @JoinTable(
            name = "group_chat_users",
            joinColumns = { @JoinColumn(name = "chat_id") },
            inverseJoinColumns = { @JoinColumn(name = "user_id") }
    )
    private List<ChatUser> users = new ArrayList<>();


    public Long getId() {
        return id;
    }

    public List<ChatUser> getUsers() {
        return users;
    }


    public void setId(Long id) {
        this.id = id;
    }

    public void setUsers(List<ChatUser> users) {
        this.users = users;
    }
}
