/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.hook.event;

import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BeforeCountEvent implements AfasEvent {

    private final SelectExpression<?> selectExpression;
}
