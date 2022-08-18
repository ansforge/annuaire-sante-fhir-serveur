/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.search.config.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The search config of a FHIR field in the FHIR resource
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
public class ResourcePathConfig {
    private String path;

    @Builder
    public ResourcePathConfig(String path) {
        this.path = path;
    }
}
