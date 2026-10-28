package com.nemouw.user.token;

import java.util.UUID;

/** A verification token that was just used up: who it was for. */
public record ConsumedToken(UUID id, UUID userId, UUID identifierId) {
}