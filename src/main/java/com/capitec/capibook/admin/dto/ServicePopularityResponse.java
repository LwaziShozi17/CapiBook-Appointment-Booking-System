package com.capitec.capibook.admin.dto;

import java.util.UUID;

public record ServicePopularityResponse(
        UUID serviceId,
        String serviceName,
        long totalBookings
) {}
