/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.utils;

import fr.ans.afas.fhirserver.hook.annotations.AfasSubscribe;
import fr.ans.afas.fhirserver.hook.annotations.AfasSubscriber;
import fr.ans.afas.fhirserver.hook.event.AfasEvent;
import fr.ans.afas.fhirserver.hook.event.BeforeCreateResourceEvent;
import fr.ans.afas.fhirserver.hook.exception.BadHookConfiguration;
import fr.ans.afas.fhirserver.hook.service.HookService;
import org.hl7.fhir.r4.model.Device;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashSet;
import java.util.Set;


/**
 * A spring bean that simulate a subscriber
 */
@AfasSubscriber
class SampleHookService {
    @AfasSubscribe
    public void on(BeforeCreateResourceEvent event) {
        HookSystemTest.events.add(event);
    }
}

/**
 * Test of the hook system with {@link  AfasSubscriber} ans {@link  AfasSubscribe} annotations
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class HookSystemTest {


    static Set<AfasEvent> events = new HashSet<>();

    @Autowired
    ApplicationContext context;

    @Autowired
    SampleHookService sampleHookService;


    /**
     * Test that when we call a hook, subscribers are notified
     */
    @Test
    public void testHookSystem() throws BadHookConfiguration {

        var hookService = new HookService(context);


        hookService.callHook(BeforeCreateResourceEvent.builder()
                .resource(new Device().setId("id-d"))
                .build());


        Assert.assertEquals(1, events.size());
        Assert.assertTrue(events.stream().findFirst().get() instanceof BeforeCreateResourceEvent);

    }


}
