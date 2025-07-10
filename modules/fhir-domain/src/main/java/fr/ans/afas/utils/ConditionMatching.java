/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.utils;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@FunctionalInterface
public interface ConditionMatching {

    static ConditionMatching ifExecute(BooleanSupplier supplier, Consumer<Boolean> action) {
        Objects.requireNonNull(action);

        return () -> {
            if (Boolean.TRUE.equals(supplier.getAsBoolean())) {
                action.accept(true);
                return true;
            }
            return false;
        };
    }

    default ConditionMatching elseIfExecute(BooleanSupplier supplier, Consumer<Boolean> action) {
        Objects.requireNonNull(action);

        return () -> {
            if (matches()) {
                return true;
            } else if (Boolean.TRUE.equals(supplier.getAsBoolean())) {
                action.accept(true);
                return true;
            }
            return false;
        };

    }

    boolean matches();
}