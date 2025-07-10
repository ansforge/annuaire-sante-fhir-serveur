/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.rass.service;

import lombok.Builder;

import java.io.Closeable;
import java.util.function.Supplier;

@Builder
public class CloseableWrapper<T extends Closeable> {

    private final Supplier<T> content;

    public T content() {
        return content.get();
    }

}
