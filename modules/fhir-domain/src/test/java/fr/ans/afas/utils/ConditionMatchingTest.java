package fr.ans.afas.utils;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class ConditionMatchingTest {

    @Test
    void testIfExecute_WithTrueCondition() {
        // Arrange
        BooleanSupplier supplier = () -> true;
        AtomicBoolean actionPerformed = new AtomicBoolean(false);
        Consumer<Boolean> action = result -> actionPerformed.set(result);

        // Act
        ConditionMatching condition = ConditionMatching.ifExecute(supplier, action);
        boolean matches = condition.matches();

        // Assert
        assertTrue(matches);
        assertTrue(actionPerformed.get());
    }

    @Test
    void testIfExecute_WithFalseCondition() {
        // Arrange
        BooleanSupplier supplier = () -> false;
        AtomicBoolean actionPerformed = new AtomicBoolean(false);
        Consumer<Boolean> action = result -> actionPerformed.set(result);

        // Act
        ConditionMatching condition = ConditionMatching.ifExecute(supplier, action);
        boolean matches = condition.matches();

        // Assert
        assertFalse(matches);
        assertFalse(actionPerformed.get());
    }

    @Test
    void testElseIfExecute_WithTrueConditions() {
        // Arrange
        AtomicBoolean firstActionPerformed = new AtomicBoolean(false);
        AtomicBoolean secondActionPerformed = new AtomicBoolean(false);

        BooleanSupplier firstSupplier = () -> true;
        Consumer<Boolean> firstAction = result -> firstActionPerformed.set(result);

        BooleanSupplier secondSupplier = () -> true;
        Consumer<Boolean> secondAction = result -> secondActionPerformed.set(result);

        ConditionMatching condition = ConditionMatching.ifExecute(firstSupplier, firstAction);

        // Act
        condition = condition.elseIfExecute(secondSupplier, secondAction);
        boolean matches = condition.matches();

        // Assert
        assertTrue(matches);
        assertTrue(firstActionPerformed.get());
        assertFalse(secondActionPerformed.get());
    }

    @Test
    void testElseIfExecute_WithFirstConditionFalseSecondConditionTrue() {
        // Arrange
        AtomicBoolean firstActionPerformed = new AtomicBoolean(false);
        AtomicBoolean secondActionPerformed = new AtomicBoolean(false);

        BooleanSupplier firstSupplier = () -> false;
        Consumer<Boolean> firstAction = result -> firstActionPerformed.set(result);

        BooleanSupplier secondSupplier = () -> true;
        Consumer<Boolean> secondAction = result -> secondActionPerformed.set(result);

        ConditionMatching condition = ConditionMatching.ifExecute(firstSupplier, firstAction);

        // Act
        condition = condition.elseIfExecute(secondSupplier, secondAction);
        boolean matches = condition.matches();

        // Assert
        assertTrue(matches);
        assertFalse(firstActionPerformed.get());
        assertTrue(secondActionPerformed.get());
    }

    @Test
    void testElseIfExecute_WithAllConditionsFalse() {
        // Arrange
        AtomicBoolean firstActionPerformed = new AtomicBoolean(false);
        AtomicBoolean secondActionPerformed = new AtomicBoolean(false);

        BooleanSupplier firstSupplier = () -> false;
        Consumer<Boolean> firstAction = result -> firstActionPerformed.set(result);

        BooleanSupplier secondSupplier = () -> false;
        Consumer<Boolean> secondAction = result -> secondActionPerformed.set(result);

        ConditionMatching condition = ConditionMatching.ifExecute(firstSupplier, firstAction);

        // Act
        condition = condition.elseIfExecute(secondSupplier, secondAction);
        boolean matches = condition.matches();

        // Assert
        assertFalse(matches);
        assertFalse(firstActionPerformed.get());
        assertFalse(secondActionPerformed.get());
    }
}
