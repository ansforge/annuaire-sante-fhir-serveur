/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet.create;

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
public class PostParams {


    /**
     * Resource type
     */
    private String resource;

    /**
     * The fhir resource
     */
    private DomainResource content;
}
