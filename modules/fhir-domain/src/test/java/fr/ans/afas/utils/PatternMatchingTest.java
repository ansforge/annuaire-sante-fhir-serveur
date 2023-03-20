/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.utils;


import org.junit.Assert;
import org.junit.Test;

/**
 * Test the PatternMatching class
 */
public class PatternMatchingTest {

    @Test
    public void testPatternMatching() {
        var match = PatternMatching
                .<Class, String>
                        when(String.class::equals, x -> "string")
                .orWhen(Number.class::equals, x -> "number")
                .otherwise(x -> "other");

        Assert.assertEquals("string", match.matches(String.class).get());
        Assert.assertEquals("number", match.matches(Number.class).get());
        Assert.assertEquals("other", match.matches(Object.class).get());

    }


}
