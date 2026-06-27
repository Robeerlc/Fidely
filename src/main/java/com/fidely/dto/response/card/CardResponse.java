package com.fidely.dto.response.card;

public record CardResponse(Long cardId, String secureUuid, Integer currentStamps, Integer maxStamps, String message) {
}