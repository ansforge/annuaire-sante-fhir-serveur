/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.hook.service;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.lang.reflect.Method;

@Getter
@NoArgsConstructor
public class EventHandler {
    /**
     * The instance containing the method to call
     */
    Object instance;

    /**
     * The method to call
     */
    Method method;

    @Builder
    public EventHandler(Object instance, Method method) {
        this.instance = instance;
        this.method = method;
    }
}
