/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.search.config.domain;

import lombok.*;

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
    @NonNull
    private String urlParameter;
    private String searchType;
    private String description;
    private List<ResourcePathConfig> resourcePaths;
    private String indexName;
    private boolean index = true;
    private boolean indexInSubRequest = false;

    @Builder
    public SearchParamConfig(String name, @NonNull String urlParameter, String searchType, String description, List<ResourcePathConfig> resourcePaths, String indexName, Boolean index, Boolean indexInSubRequest) {
        this.name = name;
        this.urlParameter = urlParameter;
        this.searchType = searchType;
        this.description = description;
        this.resourcePaths = resourcePaths;
        this.indexName = indexName;
        if (index != null) {
            this.index = index;
        }
        if (indexInSubRequest != null) {
            this.indexInSubRequest = indexInSubRequest;
        }
    }
}
