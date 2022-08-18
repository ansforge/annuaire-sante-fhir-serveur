/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.search.config.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * The search config of a FHIR parameter
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
public class SearchParamConfig {
    private String name;
    private String urlParameter;
    private String searchType;
    private String description;
    private List<ResourcePathConfig> resourcePaths;
    private String indexName;

    @Builder
    public SearchParamConfig(String name, String urlParameter, String searchType, String description, List<ResourcePathConfig> resourcePaths, String indexName) {
        this.name = name;
        this.urlParameter = urlParameter;
        this.searchType = searchType;
        this.description = description;
        this.resourcePaths = resourcePaths;
        this.indexName = indexName;
    }
}
