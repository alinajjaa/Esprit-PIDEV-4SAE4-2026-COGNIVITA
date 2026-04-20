package louzaynej.pi.pi.repositories;

import java.time.LocalDate;

public interface DayCountView {
    LocalDate getDay();
    Long getCount();
}

