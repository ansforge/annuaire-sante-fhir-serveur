/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.search.config.domain;

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
    private List<SearchParamConfig> searchParams;
}
