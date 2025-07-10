/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet.put;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.hl7.fhir.r4.model.DomainResource;


/**
 * Contains parameters of a FHIR write (put) operation
 */
@Getter
@Builder
@AllArgsConstructor
public class PutParams {


    /**
     * Resource type
     */
    private String resource;

    /**
     * Id of the resource
     */
    private String id;

    /**
     * The fhir resource
     */
    private DomainResource content;
}
