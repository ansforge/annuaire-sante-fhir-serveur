/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.hook.service;

import fr.ans.afas.fhirserver.hook.annotations.AfasSubscribe;
import fr.ans.afas.fhirserver.hook.annotations.AfasSubscriber;
import fr.ans.afas.fhirserver.hook.event.AfasEvent;
import fr.ans.afas.fhirserver.hook.exception.BadHookConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ReflectionUtils;

import java.util.*;
import java.util.stream.Collectors;

public class HookService {

    private static final String BASIC_CONFIGURATION_ERROR = "When you use the annotation @AfasSubscribe, the method annotated must have one and only one parameter that implements fr.ans.afas.fhirserver.hook.event.AfasEvent";


    /**
     * A reference to all methods that subscribes to hooks.
     * The key of the hashmap is the class of the event, the value is the list of method to call
     */
    final Map<Class<?>, List<EventHandler>> eventHandlers = new HashMap<>();


    public HookService(ApplicationContext applicationContext) throws BadHookConfiguration {
        var eventHandlersObjects = new HashSet<EventHandler>();

        var vals = applicationContext.getBeansWithAnnotation(AfasSubscriber.class).values();
        for (var val : vals) {

            eventHandlersObjects.addAll(
                    Arrays.stream(ReflectionUtils.getDeclaredMethods(val.getClass()))
                            .filter(method -> method.getAnnotation(AfasSubscribe.class) != null)
                            .map(method -> EventHandler.builder().method(method).instance(val).build())
                            .collect(Collectors.toList()));
        }

        for (var eventHandler : eventHandlersObjects) {
            var method = eventHandler.getMethod();
            var params = method.getParameters();
            if (params.length != 1) {
                throw new BadHookConfiguration(BASIC_CONFIGURATION_ERROR);
            }
            var targetEvent = params[0];
            if (!AfasEvent.class.isAssignableFrom(targetEvent.getType())) {
                throw new BadHookConfiguration(BASIC_CONFIGURATION_ERROR);
            }

            if (!this.eventHandlers.containsKey(targetEvent.getType())) {
                this.eventHandlers.put(targetEvent.getType(), new ArrayList<>());
            }
            this.eventHandlers.get(targetEvent.getType()).add(eventHandler);
        }

    }


    public void callHook(Collection<AfasEvent> events) {
        for (var e : events) {
            this.callHook(e);
        }
    }

    public void callHook(AfasEvent event) {
        if (this.eventHandlers.containsKey(event.getClass())) {
            for (var m : this.eventHandlers.get(event.getClass())) {
                if (m.getMethod().canAccess(m.instance)) {
                    ReflectionUtils.invokeMethod(m.getMethod(), m.getInstance(), event);
                }
            }
        }
    }


}
