/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet.delete;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeleteParams {
    /**
     * Resource type
     */
    private String resource;

    /**
     * Id of the resource
     */
    private String id;
}