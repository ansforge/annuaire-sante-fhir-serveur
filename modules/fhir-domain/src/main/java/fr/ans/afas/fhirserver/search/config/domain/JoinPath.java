/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.search.config.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class JoinPath {
    /**
     * The FHIR resource
     */
    String resource;
    /**
     * The search path on the resource
     */
    String path;
    /**
     * The Fhir field that reference the resource
     */
    String field;
}
