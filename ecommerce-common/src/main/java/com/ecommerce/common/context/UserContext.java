package com.ecommerce.common.context;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UserContext {
    private String userId;
    private String email;
    private String role;

    private static final ThreadLocal<UserContext> userContextThreadLocal = new ThreadLocal<>();

    public static UserContext getCurrentContext() {
        UserContext context = userContextThreadLocal.get();
        if (context == null) {
            context = new UserContext();
            userContextThreadLocal.set(context);
        }
        return context;
    }

    public static void setCurrentContext(UserContext context) {
        userContextThreadLocal.set(context);
    }

    public static void clear() {
        userContextThreadLocal.remove();
    }
}
