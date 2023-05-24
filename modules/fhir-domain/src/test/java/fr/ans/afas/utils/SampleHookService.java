/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.utils;

import fr.ans.afas.fhirserver.hook.annotations.AfasSubscribe;
import fr.ans.afas.fhirserver.hook.annotations.AfasSubscriber;
import fr.ans.afas.fhirserver.hook.event.BeforeCreateResourceEvent;

/**
 * A spring bean that simulate a subscriber
 */
@AfasSubscriber
public class SampleHookService {
    @AfasSubscribe
    public void on(BeforeCreateResourceEvent event) {
        HookSystemTest.events.add(event);
    }
}