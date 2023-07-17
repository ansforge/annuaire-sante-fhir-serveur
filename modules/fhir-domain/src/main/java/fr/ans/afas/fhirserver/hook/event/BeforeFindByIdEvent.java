/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.hook.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.instance.model.api.IIdType;

@Getter
@RequiredArgsConstructor
public class BeforeFindByIdEvent implements AfasEvent {

    private final String type;
    private final IIdType theId;
}
