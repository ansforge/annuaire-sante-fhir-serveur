/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
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
        Assert.assertEquals("FhirSearchPath{resource='r',path='a'}", one.toString());


        Assert.assertNotEquals(one.hashCode(), two.hashCode());
        Assert.assertNotEquals(one, two);
        Assert.assertNotEquals(one, twoBis);
        Assert.assertNotEquals(null, one);

    }

    @Test
    public void testFhirSearchPathNullEntries() {

        final var a = FhirSearchPath.builder();
        Assert.assertThrows(NullPointerException.class, () -> a.path(null));
        final var b = FhirSearchPath.builder().path("r");
        Assert.assertThrows(NullPointerException.class, () -> b.resource(null));
        final var c = FhirSearchPath.builder();
        Assert.assertThrows(NullPointerException.class, () -> c.path(null));
        Assert.assertThrows(NullPointerException.class, () -> new FhirSearchPath(null, null));
    }

}
