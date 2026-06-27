package com.fidely.ui.dto.card;

public record CardResponse(
        Long cardId,
        String secureUuid,
        Integer currentStamps,
        Integer maxStamps,
        String message) {
}