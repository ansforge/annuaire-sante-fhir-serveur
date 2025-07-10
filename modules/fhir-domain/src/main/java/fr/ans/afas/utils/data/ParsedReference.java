/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.utils.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Hold data of a reference
 */
@AllArgsConstructor
@Builder
@Getter
public class ParsedReference {

    /**
     * The type of the fhir resource
     */
    String resourceType;
    /**
     * The id of the fhir resource
     */
    String resourceId;

}
