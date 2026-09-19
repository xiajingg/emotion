package com.emotion.api.util;

import com.emotion.api.config.user.UserPrincipal;

public class RoleUtil {

    public static boolean judgeAdmin(UserPrincipal userPrincipal) {
        return userPrincipal.getUserId() == 1 || userPrincipal.getUserId() == 10;
    }
}
