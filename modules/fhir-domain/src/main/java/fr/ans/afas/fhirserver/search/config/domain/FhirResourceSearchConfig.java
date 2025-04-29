/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.search.config.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.StringType;

import java.util.ArrayList;
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

    private List<JoinPath> joins;

    // Añadir nuevas listas para searchInclude y searchRevInclude
    private List<StringType> searchIncludes;
    private List<StringType> searchRevIncludes;

    public FhirResourceSearchConfig() {
        this.searchIncludes = new ArrayList<>();
        this.searchRevIncludes = new ArrayList<>();
    }

    @Builder
    public FhirResourceSearchConfig(String name, String profile, List<SearchParamConfig> searchParams, List<JoinPath> joins,
                                    List<StringType> searchIncludes, List<StringType> searchRevIncludes) {
        this.name = name;
        this.profile = profile;
        this.searchParams = searchParams;
        this.joins = joins;
        this.searchIncludes = searchIncludes;
        this.searchRevIncludes = searchRevIncludes;
    }

    // Nuevo método de conveniencia para searchIncludes
    public void setSearchIncludes(String... includes) {
        this.searchIncludes = new ArrayList<>();
        for (String include : includes) {
            this.searchIncludes.add(new StringType(include));
        }
    }

    // Nuevo método de conveniencia para searchRevIncludes
    public void setSearchRevIncludes(String... revIncludes) {
        this.searchRevIncludes = new ArrayList<>();
        for (String revInclude : revIncludes) {
            this.searchRevIncludes.add(new StringType(revInclude));
        }
    }


}
