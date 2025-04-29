/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.utils;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import fr.ans.afas.fhirserver.search.exception.BadPrecisionException;

import java.time.ZoneOffset;
import java.util.Date;
import java.util.TimeZone;

/**
 * Utility class to work on date for FHIR
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public final class FhirDateUtils {

    /**
     * block the constructor of a utility class
     */
    private FhirDateUtils() {
    }

    /**
     * Get a date in a specific precision. Replace values the under the precision with 0.
     * This function may be useful to compare date with precision.
     * <p>
     * Given the date "Tue Nov 16 2021 18:36:05:123 GMT" :
     * <p>
     * In Precision of SECOND, this will return (in s) : "Tue Nov 16 2021 18:36:05 GMT"
     * In Precision of MINUTE, this will return (in s) : "Tue, 16 Nov 2021 18:36:00 GMT"
     * In Precision of DAY, this will return (in s) : "Mon, 16 Nov 2021 00:00:00 GMT"
     * ...
     *
     * @param date      the date to convert
     * @param precision the precision wanted
     * @return the date in s converted
     */
    public static long getTimeInPrecision(Date date, TemporalPrecisionEnum precision) {
        var tz = TimeZone.getDefault();
        var ldt = date.toInstant()
                .atZone(tz.toZoneId())
                .toLocalDateTime();
        var offset = ZoneOffset.ofTotalSeconds(tz.getRawOffset() / 1000);
        switch (precision) {
            case MILLI:
                return ldt.toInstant(offset).toEpochMilli();
            case SECOND:
                return ldt.withNano(0).toInstant(offset).toEpochMilli();
            case MINUTE:
                return ldt.withNano(0).withSecond(0).toInstant(offset).toEpochMilli();
            case DAY:
                return ldt.withNano(0).withMinute(0).withHour(0).withSecond(0).toInstant(offset).toEpochMilli();
            case MONTH:
                return ldt.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant(offset).toEpochMilli();
            case YEAR:
                return ldt.withDayOfYear(1).withNano(0).withMinute(0).withHour(0).withSecond(0).withNano(0).toInstant(offset).toEpochMilli();
            default:
                throw new BadPrecisionException("Unsupported date precision: " + precision);
        }
    }

}
