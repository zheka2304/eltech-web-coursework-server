package com.eltech.web.server.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.function.Function;

@Entity
public class Dialog {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name="user_id", nullable=false)
    private ChatUser user;

    // for peer-to-peer chat target is just uid
    private String target;

    // target user by target uid is cached for faster access
    @Transient private boolean targetUserCached = false;
    @Transient private ChatUser targetUser;


    public Dialog() {

    }

    public Dialog(ChatUser user, String target) {
        this.user = user;
        this.target = target;
    }


    public Long getId() {
        return id;
    }

    @JsonIgnore
    public ChatUser getUser() {
        return user;
    }

    public String getTarget() {
        return target;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUser(ChatUser user) {
        this.user = user;
    }

    public void setTarget(String target) {
        this.target = target;
        targetUserCached = false;
    }

    public ChatUser getTargetUser(Function<String, ChatUser> supplier, boolean forceReCache) {
        if (!targetUserCached || forceReCache) {
            targetUser = supplier.apply(getTarget());
            targetUserCached = true;
        }
        return targetUser;
    }
}
