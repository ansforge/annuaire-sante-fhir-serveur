/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.search.config.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;

/**
 * The search config of the server
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Getter
@Setter
public class ServerSearchConfig {
    private String validationMode;
    private String implementationGuideUrl;
    private String copyright;
    private Collection<FhirResourceSearchConfig> resources;

    public ServerSearchConfig() {
        this.resources = new ArrayList<>();
    }

    @Builder
    public ServerSearchConfig(String validationMode, Collection<FhirResourceSearchConfig> resources) {
        this.validationMode = validationMode;
        this.resources = resources == null ? new ArrayList<>() : resources;
    }
}
