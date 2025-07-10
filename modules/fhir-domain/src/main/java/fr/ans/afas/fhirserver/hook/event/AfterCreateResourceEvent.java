/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.hook.event;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hl7.fhir.instance.model.api.IBaseResource;

@Getter
@NoArgsConstructor
public class AfterCreateResourceEvent implements AfasEvent {

    IBaseResource resource;

    @Builder
    public AfterCreateResourceEvent(IBaseResource resource) {
        this.resource = resource;
    }

}
