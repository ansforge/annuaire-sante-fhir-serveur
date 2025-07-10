package fr.ans.afas.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MongoDbUtilsTest {

    @Test
    void testRemoveAccentsAndLowerCase() {
        // Test with accents and uppercase letters
        String input = "Élève À l'école";
        String expected = "eleve a l'ecole";
        String result = MongoDbUtils.removeAccentsAndLowerCase(input);
        assertEquals(expected, result);

        // Test with null input
        assertNull(MongoDbUtils.removeAccentsAndLowerCase(null));

        // Test with empty string
        assertEquals("", MongoDbUtils.removeAccentsAndLowerCase(""));

        // Test with no accents and lowercase letters
        input = "hello world";
        expected = "hello world";
        result = MongoDbUtils.removeAccentsAndLowerCase(input);
        assertEquals(expected, result);
    }
}