/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.search.config.domain;

import lombok.*;

import java.util.Map;

/**
 * The search config of the server
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
public class ServerSearchConfig {

    private Map<String, TenantSearchConfig> configs;

    @Builder
    public ServerSearchConfig(@NonNull Map<String, TenantSearchConfig> configs) {
        this.configs = configs;
    }
}
