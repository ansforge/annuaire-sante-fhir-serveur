/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.search.config.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * The fhir configuration for a resource
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Getter
@Setter
public class FhirResourceSearchConfig {

    private String name;
    private String profile;
    private boolean visible = true;
    private boolean canRead = true;
    private boolean canWrite = true;
    private boolean canDelete = true;

    private List<SearchParamConfig> searchParams;


    public FhirResourceSearchConfig() {

    }

    @Builder
    public FhirResourceSearchConfig(String name, String profile, List<SearchParamConfig> searchParams) {
        this.name = name;
        this.profile = profile;
        this.searchParams = searchParams;
    }
}
