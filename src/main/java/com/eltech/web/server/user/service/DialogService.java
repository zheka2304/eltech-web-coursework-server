package com.eltech.web.server.user.service;

import com.eltech.web.server.user.entity.ChatUser;
import com.eltech.web.server.user.entity.Dialog;
import com.eltech.web.server.user.repo.UserRepository;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Objects;

@Service
public class DialogService {
    private final UserService userService;

    public DialogService(UserService userService) {
        this.userService = userService;
    }

    public Dialog getDialog(ChatUser user, String target) {
        if (user == null) {
            return null;
        }
        for (Dialog dialog : user.getDialogs()) {
            if (target.equals(dialog.getTarget())) {
                return dialog;
            }
        }
        return null;
    }

    @Transactional
    public Dialog getOrAddDialog(ChatUser user, String target) {
        // if dialog already exists, just update and return it
        Dialog dialog = getDialog(user, target);
        if (dialog != null) {
            dialog.updateLastActivityTime();
            userService.save(user);
            return dialog;
        }

        // save dialog and fetch user from database
        dialog = new Dialog(user, target);
        dialog.updateLastActivityTime();
        user.getDialogs().add(dialog);
        user = userService.saveAndFetch(user);

        // dialog is added, return it
        return getDialog(user, target);
    }

    public boolean removeDialog(ChatUser user, Dialog dialog) {
        boolean result = user.getDialogs().removeIf(_dialog -> Objects.equals(dialog.getId(), _dialog.getId()));
        if (result) {
            user.setDialogs(user.getDialogs());
            userService.save(user);
        }
        return result;
    }
}
