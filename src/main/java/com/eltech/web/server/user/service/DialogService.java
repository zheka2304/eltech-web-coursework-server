package com.eltech.web.server.user.service;

import com.eltech.web.server.user.entity.ChatUser;
import com.eltech.web.server.user.entity.Dialog;
import com.eltech.web.server.user.repo.UserRepository;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

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
        Dialog dialog = getDialog(user, target);
        if (dialog != null) {
            return dialog;
        }

        // save dialog and fetch user from database
        dialog = new Dialog(user, target);
        user.getDialogs().add(dialog);
        user = userService.saveAndFetch(user);

        // dialog is added, return it
        return getDialog(user, target);
    }

    public boolean removeDialog(ChatUser user, String target) {
        boolean result = user.getDialogs().removeIf(dialog -> dialog.getTarget().equals(target));
        if (result) {
            user.setDialogs(user.getDialogs());
            userService.save(user);
        }
        return result;
    }
}
