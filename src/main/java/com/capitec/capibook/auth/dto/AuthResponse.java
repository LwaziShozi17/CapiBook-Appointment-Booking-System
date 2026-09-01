package com.capitec.capibook.auth.dto;

import com.capitec.capibook.user.dto.UserProfileResponse;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserProfileResponse user
) {
}
