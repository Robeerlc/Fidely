package com.fidely.dto;

public record CardResponse(Long cardId, String secureUuid, Integer currentStamps, Integer maxStamps, String message) {
}