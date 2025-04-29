/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet.read;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Contains parameters of a FHIR read operation
 */
@Getter
@Builder
@AllArgsConstructor
public class ReadSearchParams {

    /**
     * Resource
     */
    private String resource;

    /**
     * Id of the resource
     */
    private String id;

}
