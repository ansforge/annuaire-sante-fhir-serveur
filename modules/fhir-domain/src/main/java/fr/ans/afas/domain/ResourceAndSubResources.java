/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.domain;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.hl7.fhir.r4.model.DomainResource;

import java.util.List;

/**
 * A domain resource and its related resources.
 * Used to store a resource
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Getter
@Builder
@AllArgsConstructor
public class ResourceAndSubResources {
    DomainResource resource;
    List<DomainResource> subResources;
}
