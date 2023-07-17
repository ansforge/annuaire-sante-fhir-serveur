/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.search.config;

import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.domain.FhirResourceSearchConfig;
import fr.ans.afas.fhirserver.search.config.domain.JoinPath;
import fr.ans.afas.fhirserver.search.config.domain.SearchParamConfig;
import fr.ans.afas.fhirserver.search.config.domain.ServerSearchConfig;

import java.util.*;

/**
 * Base class for a search config service.
 * Implements {@link SearchConfig} methods based on a config passed into the constructor
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public abstract class BaseSearchConfigService implements SearchConfig {


    /**
     * The configuration of the service
     */
    protected final Map<String, FhirResourceSearchConfig> configs;

    protected final ServerSearchConfig serverSearchConfig;

    /**
     * Construct the search config
     *
     * @param serverSearchConfig the configuration
     */
    protected BaseSearchConfigService(ServerSearchConfig serverSearchConfig) {
        this.serverSearchConfig = serverSearchConfig;
        this.configs = new HashMap<>();
        for (var r : serverSearchConfig.getResources()) {
            this.configs.put(r.getName(), r);
        }
    }

    /**
     * Get all configurations for a specific resource
     *
     * @param fhirResource the resource
     * @return the list of configurations
     */
    @Override
    public List<SearchParamConfig> getAllByFhirResource(String fhirResource) {
        return configs.get(fhirResource).getSearchParams();
    }


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

        if (paramName.startsWith("links")) {
            var parts = paramName.split("\\.");
            if (parts.length != 3) {
                return Optional.empty();
            }

            return configs.get(parts[1])
                    .getSearchParams()
                    .stream()
                    .filter(conf -> conf.getUrlParameter().equals(parts[2]))
                    .map(p -> {
                        var r = new SearchParamConfig();
                        r.setName(r.getName());
                        r.setIndexName(parts[0] + "." + parts[1] + "." + p.getIndexName());
                        r.setResourcePaths(p.getResourcePaths());
                        r.setUrlParameter(p.getUrlParameter());
                        return r;
                    })
                    .findAny();

        } else {
            return configs.get(resourceType)
                    .getSearchParams()
                    .stream()
                    .filter(conf -> conf.getUrlParameter().equals(paramName))
                    .findAny();
        }
    }

    @Override
    public Set<String> getResources() {
        return configs.keySet();
    }

    @Override
    public ServerSearchConfig getServerSearchConfig() {
        this.serverSearchConfig.setResources(configs.values());
        return this.serverSearchConfig;
    }

    @Override
    public List<JoinPath> getJoinsByFhirResource(String fhirResource) {
        if (!configs.containsKey(fhirResource)) {
            return List.of();
        }
        return configs.get(fhirResource)
                .getJoins();
    }


}
