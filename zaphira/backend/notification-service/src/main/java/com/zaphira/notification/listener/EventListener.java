package com.zaphira.notification.listener;

import com.zaphira.common.event.UserRegisteredEvent;

public interface EventListener {
    void handleUserRegistered(UserRegisteredEvent event);
}