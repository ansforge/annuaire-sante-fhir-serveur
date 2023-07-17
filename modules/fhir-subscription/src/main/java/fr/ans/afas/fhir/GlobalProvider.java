/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir;

import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import fr.ans.afas.service.SubscriptionOperationService;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.OperationOutcome;

public class GlobalProvider {

    /**
     * The subscription operation service
     */
    final SubscriptionOperationService subscriptionOperationService;

    public GlobalProvider(SubscriptionOperationService subscriptionOperationService) {
        this.subscriptionOperationService = subscriptionOperationService;
    }


    @Operation(name = "$admin-patch-server-configuration")
    public OperationOutcome adminPatchServerConfiguration(
            @OperationParam(name = "subscriptionsActivated") BooleanType activated
    ) {
        this.subscriptionOperationService.setSubscriptionsEnabled(activated.booleanValue());

        return new OperationOutcome();
    }

}
