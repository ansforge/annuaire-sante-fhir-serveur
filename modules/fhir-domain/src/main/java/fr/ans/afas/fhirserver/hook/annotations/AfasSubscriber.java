/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.hook.annotations;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
public @interface AfasSubscriber {
}
