/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.search.config.domain;

import lombok.*;

import javax.validation.constraints.NotNull;
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
@Builder
@AllArgsConstructor
public class SearchParamConfig {
    @NotNull
    private String name;
    @NotNull
    private String urlParameter;
    @NotNull
    private String searchType;
    //Name used to search in Mongo for ReferenceExpression. Ex: Organization/ID_1
    private String referenceType;
    //It's used to add always in response of a search that contains param _elements. Even if it's not included in param _elements
    private Boolean isCompulsoryOrModifierElementsParam;
    private String description;
    private String definition;
    //ResourcePaths is used for search inside attribute fhir of resource and put the value found in his corresponding index when saving resource in db (ex: t_id...)
    @NotNull
    private List<ResourcePathConfig> resourcePaths;
    @NotNull
    private String indexName;
    @Builder.Default
    private boolean index = true;
    @Builder.Default
    private boolean indexInSubRequest = false;

}
