/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.utils;

import fr.ans.afas.fhirserver.search.FhirSearchPath;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test fhir path system
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class FhirSearchPathTest {

    @Test
    public void testFhirSearchPathBean() {
        var one = FhirSearchPath.builder().path("a").resource("r").build();
        var oneBis = FhirSearchPath.builder().path("a").resource("r").build();
        var two = FhirSearchPath.builder().path("b").resource("r").build();
        var twoBis = FhirSearchPath.builder().path("a").resource("c").build();

        Assert.assertEquals("a", oneBis.getPath());
        Assert.assertEquals("r", oneBis.getResource());
        Assert.assertEquals(one, oneBis);
        Assert.assertEquals(one.hashCode(), oneBis.hashCode());
        Assert.assertEquals("FhirSearchPath{resource='r', path='a'}", one.toString());


        Assert.assertNotEquals(one.hashCode(), two.hashCode());
        Assert.assertNotEquals(one, two);
        Assert.assertNotEquals(one, twoBis);
        Assert.assertNotEquals(null, one);

    }

    @Test
    public void testFhirSearchPathNullEntries() {

        Assert.assertThrows(NullPointerException.class, () -> {
            FhirSearchPath.builder().path(null).resource("r").build();
        });
        Assert.assertThrows(NullPointerException.class, () -> {
            FhirSearchPath.builder().path("r").resource(null).build();
        });
        Assert.assertThrows(NullPointerException.class, () -> {
            FhirSearchPath.builder().path(null).resource(null).build();
        });
        Assert.assertThrows(NullPointerException.class, () -> {
            new FhirSearchPath(null, null);
        });
    }

}
