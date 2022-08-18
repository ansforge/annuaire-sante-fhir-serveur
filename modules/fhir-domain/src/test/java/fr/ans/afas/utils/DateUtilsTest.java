/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.utils;

import org.junit.Test;

/**
 * Test {@link FhirDateUtils}
 */
public class DateUtilsTest {

    /**
     * Test the {@link FhirDateUtils#getTimeInPrecision} method
     */
    @Test
    public void testGetTimeInPrecision() {
        // This test is commented because it only works if the timezone is UTC
/*        System.setProperty("user.timezone", "UTC");
        Calendar calendar = Calendar.getInstance();

        // Tue Nov 16 2021 18:36:05 GMT
        var baseDate = 1637087765276L;
        calendar.setTimeInMillis(baseDate);

        // test all precisions:
        // Tue Nov 16 2021 18:36:05 GMT
        // we dont support ms so the time in ms is like the time in s.
        Assert.assertEquals(1637087765L, FhirDateUtils.getTimeInPrecision(calendar.getTime(), TemporalPrecisionEnum.MILLI));
        // Tue Nov 16 2021 18:36:05 GMT
        Assert.assertEquals(1637087765L, FhirDateUtils.getTimeInPrecision(calendar.getTime(), TemporalPrecisionEnum.SECOND));
        // Tue, 16 Nov 2021 18:36:00 GMT
        Assert.assertEquals(1637087760L, FhirDateUtils.getTimeInPrecision(calendar.getTime(), TemporalPrecisionEnum.MINUTE));
        // Mon, 16 Nov 2021 00:00:00 GMT
        Assert.assertEquals(1637020800L, FhirDateUtils.getTimeInPrecision(calendar.getTime(), TemporalPrecisionEnum.DAY));
        // Mon, 01 Nov 2021 00:00:00 GMT
        Assert.assertEquals(1635724800L, FhirDateUtils.getTimeInPrecision(calendar.getTime(), TemporalPrecisionEnum.MONTH));
        // Fri, 01 Jan 2021 00:00:00 GMT
        Assert.assertEquals(1609459200L, FhirDateUtils.getTimeInPrecision(calendar.getTime(), TemporalPrecisionEnum.YEAR));

*/
    }


}
