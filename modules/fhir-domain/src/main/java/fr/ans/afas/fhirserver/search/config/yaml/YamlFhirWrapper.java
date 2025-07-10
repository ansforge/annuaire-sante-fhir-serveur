/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.search.config.yaml;

import fr.ans.afas.fhirserver.search.config.domain.TenantSearchConfig;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class YamlFhirWrapper {
    TenantSearchConfig fhir;
}
