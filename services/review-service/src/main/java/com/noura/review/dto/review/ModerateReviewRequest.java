package com.noura.review.dto.review;

import jakarta.validation.constraints.Size;

/**
 * Admin moderation note payload for approve/reject actions.
 *
 * @param moderationNotes optional moderator notes
 */
public record ModerateReviewRequest(
        @Size(max = 1000, message = "moderationNotes must be 1000 characters or fewer")
        String moderationNotes
) {
}
