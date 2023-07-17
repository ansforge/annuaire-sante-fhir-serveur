/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.hook.event;

import lombok.Builder;
import lombok.Getter;
import org.hl7.fhir.instance.model.api.IIdType;

@Getter
@Builder
public class AfterDeleteEvent implements AfasEvent {

    private final IIdType resourceId;
}
