package com.capitec.capibook.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class BookingMetricsService {

    private final Counter appointmentsBooked;
    private final Counter appointmentsCancelled;
    private final Counter appointmentsConfirmed;
    private final Counter appointmentsCompleted;
    private final Counter appointmentsNoShow;
    private final Counter appointmentsRescheduled;

    public BookingMetricsService(MeterRegistry registry) {
        this.appointmentsBooked = Counter.builder("capibook.appointments.booked")
                .description("Total number of appointments booked")
                .register(registry);
        this.appointmentsCancelled = Counter.builder("capibook.appointments.cancelled")
                .description("Total number of appointments cancelled")
                .register(registry);
        this.appointmentsConfirmed = Counter.builder("capibook.appointments.confirmed")
                .description("Total number of appointments confirmed by staff")
                .register(registry);
        this.appointmentsCompleted = Counter.builder("capibook.appointments.completed")
                .description("Total number of appointments completed")
                .register(registry);
        this.appointmentsNoShow = Counter.builder("capibook.appointments.no_show")
                .description("Total number of appointments marked as no-show")
                .register(registry);
        this.appointmentsRescheduled = Counter.builder("capibook.appointments.rescheduled")
                .description("Total number of appointments rescheduled")
                .register(registry);
    }

    public void incrementBooked() {
        appointmentsBooked.increment();
    }

    public void incrementCancelled() {
        appointmentsCancelled.increment();
    }

    public void incrementConfirmed() {
        appointmentsConfirmed.increment();
    }

    public void incrementCompleted() {
        appointmentsCompleted.increment();
    }

    public void incrementNoShow() {
        appointmentsNoShow.increment();
    }

    public void incrementRescheduled() {
        appointmentsRescheduled.increment();
    }
}
