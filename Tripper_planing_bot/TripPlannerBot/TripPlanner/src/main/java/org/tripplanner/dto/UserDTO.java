package org.tripplanner.dto;

import java.time.Instant;

public record UserDTO(Long chatId, Instant createdAt) {}
