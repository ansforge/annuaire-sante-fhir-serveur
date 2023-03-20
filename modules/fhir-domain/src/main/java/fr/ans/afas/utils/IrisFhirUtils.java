/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.utils;

import fr.ans.afas.exception.BadReferenceFormat;
import fr.ans.afas.utils.data.ParsedReference;
import org.springframework.util.Assert;

/**
 * Fhir utility class
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public final class IrisFhirUtils {

    private IrisFhirUtils() {
    }

    public static ParsedReference parseReference(String reference) throws BadReferenceFormat {
        Assert.hasLength(reference, "The reference must not be null");
        var parts = reference.split("/");
        if (parts.length < 2) {
            throw new BadReferenceFormat(reference);
        }
        return ParsedReference.builder()
                .resourceType(parts[0])
                .resourceId(parts[1])
                .build();
    }
}
