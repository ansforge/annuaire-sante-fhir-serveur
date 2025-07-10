/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.utils;

import java.text.Normalizer;

public class MongoDbUtils {


    private MongoDbUtils() {
        throw new IllegalStateException("Utility class");
    }


    /**
     * Normalise une chaîne de caractères :
     * <ul>
     *   <li>Supprime les accents (diacritiques)</li>
     *   <li>Convertit en minuscules</li>
     * </ul>
     *
     * @param input la chaîne d'entrée (peut être null)
     * @return la chaîne normalisée (ou null si input == null)
     */


    public static String removeAccentsAndLowerCase(String input) {
        if (input == null) {
            return null;
        }
        // 1) Décomposer les caractères accentués (NFD)
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        // 2) Supprimer les diacritiques (combining marks)
        String withoutAccents = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        // 3) Mettre en minuscules
        return withoutAccents.toLowerCase();
    }
}
