/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.utils;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import org.junit.Assert;
import org.junit.Test;

import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Test {@link FhirDateUtils}
 */
public class DateUtilsTest {

    /**
     * Test the {@link FhirDateUtils#getTimeInPrecision} method
     */
    @Test
    public void testGetTimeInPrecision() {
        Calendar calendar = Calendar.getInstance();

        // Tue Nov 16 2021 18:36:05 GMT
        var baseDate = 1637087765276L;
        calendar.setTimeInMillis(baseDate);
        var offset = ZoneOffset.ofTotalSeconds(TimeZone.getDefault().getRawOffset() / 1000).getTotalSeconds() * 1000L;

        // test all precisions:
        // Tue Nov 16 2021 18:36:05 GMT
        // we dont support ms so the time in ms is like the time in s.
        Assert.assertEquals(1637087765276L, FhirDateUtils.getTimeInPrecision(calendar.getTime(), TemporalPrecisionEnum.MILLI));
        // Tue Nov 16 2021 18:36:05 GMT
        Assert.assertEquals(1637087765000L, FhirDateUtils.getTimeInPrecision(calendar.getTime(), TemporalPrecisionEnum.SECOND));
        // Tue, 16 Nov 2021 18:36:00 GMT
        Assert.assertEquals(1637087760000L, FhirDateUtils.getTimeInPrecision(calendar.getTime(), TemporalPrecisionEnum.MINUTE));
        // Mon, 16 Nov 2021 00:00:00 GMT
        Assert.assertEquals(1637020800000L - offset, FhirDateUtils.getTimeInPrecision(calendar.getTime(), TemporalPrecisionEnum.DAY));
        // Mon, 01 Nov 2021 00:00:00 GMT
        Assert.assertEquals(1635724800000L - offset, FhirDateUtils.getTimeInPrecision(calendar.getTime(), TemporalPrecisionEnum.MONTH));
        // Fri, 01 Jan 2021 00:00:00 GMT
        Assert.assertEquals(1609459200000L - offset, FhirDateUtils.getTimeInPrecision(calendar.getTime(), TemporalPrecisionEnum.YEAR));

    }


}
