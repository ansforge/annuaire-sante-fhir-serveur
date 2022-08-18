/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.search.config;

import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.domain.SearchParamConfig;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Base classe for a search config service.
 * Implements {@link SearchConfig} methods based on a config passed into the constructor
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public abstract class BaseSearchConfigService implements SearchConfig {


    /**
     * The configuration of the service
     */
    protected final Map<String, List<SearchParamConfig>> configs;

    /**
     * Construct the search config
     *
     * @param configs the configuration as a map. The key of the map is the Fhir resource name.
     */
    protected BaseSearchConfigService(Map<String, List<SearchParamConfig>> configs) {
        this.configs = configs;
    }

    /**
     * Get all configurations for a specific resource
     *
     * @param fhirResource the resource
     * @return the list of configurations
     */
    @Override
    public List<SearchParamConfig> getAllByFhirResource(String fhirResource) {
        return configs.get(fhirResource);
    }


    /**
     * Get a mapping configuration with a full path
     *
     * @param fhirResourcePath the fill path "Resource.path" of the resource to find
     * @return the configuration (empty if not found)

     @Override public Optional<SearchParamConfig> getSearchConfigByFullPath(String fhirResourcePath) {
     var toFindParts = fhirResourcePath.split("\\.");
     if (toFindParts.length < 2) {
     throw new BadConfigurationException("The fhir resource path must contains the Fhir Resource and a property path. A valid example is Organization.name. Given: " + fhirResourcePath);
     }
     var fhirResource = toFindParts[0];
     if (!configs.containsKey(fhirResource)) {
     return Optional.empty();
     }
     // we have to compare ignore case because fhir talk in small case for params and camelcase for properties:
     return configs.get(fhirResource).stream().filter(conf -> conf.getFhirParamExpression().equalsIgnoreCase(fhirResourcePath)).findAny();
     }     */

    /**
     * Get a mapping configuration with a FhirSearchPath
     *
     * @param path the path
     * @return the configuration (empty if not found)
     */
    @Override
    public Optional<SearchParamConfig> getSearchConfigByPath(FhirSearchPath path) {
        if (!configs.containsKey(path.getResource())) {
            return Optional.empty();
        }
        return getSearchConfigByResourceAndParamName(path.getResource(), path.getPath());
    }

    /**
     * Get a mapping configuration with a Param name and resource
     *
     * @param resourceType the type of the fhir resource
     * @param paramName    the name of the fhir param
     * @return the configuration (empty if not found)
     */
    @Override
    public Optional<SearchParamConfig> getSearchConfigByResourceAndParamName(String resourceType, String paramName) {
        if (!configs.containsKey(resourceType)) {
            return Optional.empty();
        }
        return configs.get(resourceType)
                .stream()
                .filter(conf -> conf.getUrlParameter().equals(paramName))
                .findAny();
    }

    @Override
    public Set<String> getResources() {
        return configs.keySet();
    }


}
