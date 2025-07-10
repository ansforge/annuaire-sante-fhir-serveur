/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.search.config.domain;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {
    String name;
    // TODO validate that the path must start with /
    String path;
    String dbname;
    String suffixCollection;
    boolean isDefault = false;
}