package dev.floofy.noel.birthdays.api;

import java.time.Instant;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.ZoneId;

/// Works out when a birthday happens, as an instant, in the birthday person's
/// timezone.
///
/// A birthday is celebrated for the whole local day, from the start of that day
/// in the person's zone until the start of the next. The rules:
///
/// - **Leap days.** A 29 February birthday is celebrated on 28 February in
///   non-leap years, which is what [MonthDay#atYear(int)] does.
/// - **DST gaps.** In a few zones, midnight doesn't exist on the day DST starts
///   (`America/Santiago`, for example). The day then starts at the first valid
///   local time, as [LocalDate#atStartOfDay(ZoneId)] resolves it.
/// - **Once per year.** An occurrence is only returned for a year after the last
///   one celebrated, so changing timezone on the day of a birthday can't
///   celebrate it twice.
public final class Calendar {
    private Calendar() {}

    /// Returns the start of the next birthday day that hasn't finished yet at
    /// `now`, in a year after `lastCelebratedYear`.
    ///
    /// If the birthday is today in `zone` and hasn't been celebrated, the result
    /// is the start of today, which is in the past. That is deliberate: setting a
    /// birthday on the day itself still celebrates it.
    ///
    /// @param birthday           the birthday
    /// @param zone               the person's timezone
    /// @param now                the current instant
    /// @param lastCelebratedYear the last year it was celebrated, or `0` if never
    /// @return the instant the next uncelebrated birthday day starts
    public static Instant next(MonthDay birthday, ZoneId zone, Instant now, int lastCelebratedYear) {
        // Start a year early: in zones far ahead of or behind `now`'s UTC date, the
        // local year can differ, and a birthday day spans more than one UTC date.
        var year = Math.max(LocalDate.ofInstant(now, zone).getYear() - 1, lastCelebratedYear + 1);
        while (true) {
            var date = birthday.atYear(year);
            if (endOfDay(date, zone).isAfter(now)) {
                return date.atStartOfDay(zone).toInstant();
            }

            year++;
        }
    }

    /// Returns when the birthday day starting at `start` ends.
    /// @param start a value returned by [#next]
    /// @param zone  the same zone passed to [#next]
    /// @return the start of the following local day
    public static Instant endOfDay(Instant start, ZoneId zone) {
        return endOfDay(LocalDate.ofInstant(start, zone), zone);
    }

    /// Returns the year of the birthday day starting at `start`.
    /// @param start a value returned by [#next]
    /// @param zone  the same zone passed to [#next]
    public static int yearOf(Instant start, ZoneId zone) {
        return LocalDate.ofInstant(start, zone).getYear();
    }

    private static Instant endOfDay(LocalDate date, ZoneId zone) {
        return date.plusDays(1).atStartOfDay(zone).toInstant();
    }
}
