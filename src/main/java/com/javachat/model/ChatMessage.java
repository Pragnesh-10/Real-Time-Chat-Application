package com.javachat.model;

import java.time.LocalDateTime;

public record ChatMessage(
        long id,
        int senderId,
        String senderUsername,
        int recipientId,
        String recipientUsername,
        String text,
        LocalDateTime sentAt
) {}
