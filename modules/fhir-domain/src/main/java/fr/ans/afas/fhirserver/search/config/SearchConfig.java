/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.search.config;

import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.domain.SearchParamConfig;
import fr.ans.afas.fhirserver.search.config.domain.ServerSearchConfig;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Configuration of the Mapping of FHIR resources
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public interface SearchConfig {

    /**
     * Get configuration for the server
     *
     * @return the configuration
     */
    ServerSearchConfig getServerSearchConfig();

    /**
     * Get all configurations for a specific resource
     *
     * @param fhirResource the resource
     * @return the list of configurations
     */
    List<SearchParamConfig> getAllByFhirResource(String fhirResource);

    /**
     * Get a mapping configuration with a FhirSearchPath
     *
     * @param path the path
     * @return the configuration (empty if not found)
     */
    Optional<SearchParamConfig> getSearchConfigByPath(FhirSearchPath path);


    /**
     * Search a configuration with a resource type and a fhir param
     *
     * @param resourceType the resource type
     * @param paramName    the fhir param name
     * @return the configuration (empty if not found)
     */
    Optional<SearchParamConfig> getSearchConfigByResourceAndParamName(String resourceType, String paramName);

    /**
     * Return the Set of configured FHIR resources (Device, Organization...)
     *
     * @return configured resources names
     */
    Set<String> getResources();
}
