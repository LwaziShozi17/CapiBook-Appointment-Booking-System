package com.capitec.capibook.availability.dto;

import com.capitec.capibook.availability.SlotStatus;

import java.time.LocalTime;

public record SlotResponse(LocalTime startTime, LocalTime endTime, SlotStatus status) {}
