package com.kshitiz.taskforge.ai.application.dto;

/**
 * Response of {@code GET /api/ai/status}, so the frontend can show or hide AI buttons and
 * display the remaining allowance.
 *
 * @param enabled        whether AI features are available
 * @param provider       provider name, e.g. "google-genai", "fake" or "none"
 * @param dailyLimit     AI requests per user per day
 * @param remainingToday requests the current user has left today
 */
public record AiStatusResponse(boolean enabled, String provider, int dailyLimit, int remainingToday) {
}
