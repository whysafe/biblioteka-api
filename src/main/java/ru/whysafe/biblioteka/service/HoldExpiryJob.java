package ru.whysafe.biblioteka.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Раз в сутки снимает брони, которые не забрали за hold-days, и передаёт экземпляр следующему в очереди. */
@Component
public class HoldExpiryJob {

    private final ReservationService reservations;

    public HoldExpiryJob(ReservationService reservations) {
        this.reservations = reservations;
    }

    @Scheduled(cron = "${app.library.expire-holds-cron}")
    public void run() {
        reservations.expireHolds();
    }
}
