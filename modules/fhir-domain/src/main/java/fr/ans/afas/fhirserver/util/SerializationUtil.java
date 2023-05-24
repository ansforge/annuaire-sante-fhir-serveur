/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.util;

public class SerializationUtil {

    private SerializationUtil() {
    }

    public static String wrapBundleEntry(String content) {
        return "{\n" +
                "\"fullUrl\":" +
                "\"https://example.com/base/MedicationRequest/3123\",\n" +
                "\"resource\":" +
                content +
                "}";
    }
}
