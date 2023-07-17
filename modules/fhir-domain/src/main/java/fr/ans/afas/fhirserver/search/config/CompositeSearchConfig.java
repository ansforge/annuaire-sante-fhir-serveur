/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.search.config;

import fr.ans.afas.fhirserver.search.config.domain.ServerSearchConfig;

import java.util.List;


/**
 * A search config {@link SearchConfig} base on a list of {@link ServerSearchConfig}.
 * This class will merge configuration from all configs.
 * If there is multiple definition of the same configuration, the first one is override.
 */
public class CompositeSearchConfig extends BaseSearchConfigService {

    /**
     * Construct the search config from a list of search config
     *
     * @param searchConfigs search config to merge
     */
    public CompositeSearchConfig(List<ServerSearchConfig> searchConfigs) {
        super(searchConfigs.get(0));
        // merge all search configs
        for (var searchConfig : searchConfigs) {
            for (var resourceSearchConfig : searchConfig.getResources()) {
                this.configs.put(resourceSearchConfig.getName(), resourceSearchConfig);
            }
        }


    }
}
