/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.utils;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

@FunctionalInterface
public interface SelectExpressionMatching<O, T, P, U, R> {

    static <O, T, P, U, R> SelectExpressionMatching<O, T, P, U, R> when(Predicate<T> predicate, TriFunction<O, P, U, R> action) {
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(action);

        return (orExpression, check, path, param) -> {
            if (predicate.test(check)) {
                return Optional.of(action.apply(orExpression, path, param));
            } else {
                return Optional.empty();
            }
        };
    }

    default SelectExpressionMatching<O, T, P, U, R> orWhen(Predicate<T> predicate, TriFunction<O, P, U, R> action) {
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(action);

        return (orExpression, check, path, param) -> {
            final Optional<R> result = addOrExpression(orExpression, check, path, param);
            if (result.isPresent()) {
                return result;
            } else {
                if (predicate.test(check)) {
                    return Optional.of(action.apply(orExpression, path, param));
                } else {
                    return Optional.empty();
                }
            }
        };
    }

    Optional<R> addOrExpression(O orExpression, T check, P path, U param);

}