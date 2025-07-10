/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.validation;

import fr.ans.afas.exception.BadDataFormatException;

import java.util.function.Predicate;

import static java.util.regex.Pattern.compile;

/**
 * Utility class to validate fhir data.
 * This class is based on <a href="https://www.hl7.org/fhir/datatypes.html">HL7 website</a>
 */
public class DataValidationUtils {

    /**
     * Hl7 Fhir regex for canonical
     */
    static final Predicate<String> patternCanonical = compile("^\\S*$").asPredicate();
    /**
     * Hl7 Fhir regex for code (this is not the real pattern but this pattern perform better)
     */
    static final Predicate<String> patternCode = compile("^[\\r\\n\\t\\u0020-\\uFFFF]*$").asPredicate();
    /**
     * Hl7 Fhir regex for String
     */
    static final Predicate<String> patternString = compile("^[\\r\\n\\t\\u0020-\\uFFFF]*$").asPredicate();
    /**
     * Hl7 Fhir regex for decimal
     */
    static final Predicate<String> patternDecimal = compile("^-?(0|[1-9]\\d{0,17})(\\.\\d{1,17})?([eE][+-]?\\d{1,9}})?$").asPredicate();
    /**
     * Hl7 Fhir regex for date time
     * The original one is : ^([0-9]([0-9]([0-9][1-9]|[1-9]0)|[1-9]00)|[1-9]000)(-(0[1-9]|1[0-2])(-(0[1-9]|[1-2][0-9]|3[0-1])(T([01][0-9]|2[0-3]):[0-5][0-9]:([0-5][0-9]|60)(\.[0-9]{1,9})?)?)?(Z|(\+|-)((0[0-9]|1[0-3]):[0-5][0-9]|14:00)?)?)?$
     * But is too complex so we use a simplified one
     */
    static final Predicate<String> patternDateTime = compile("^\\d{4}(-\\d{2})?(-\\d{2})?(T\\d{2})?[-+0-9:.]{0,18}$").asPredicate();
    /**
     * A regex to validate parameters
     */
    static final Predicate<String> patternParameters = compile("^[\\u0020-\\uFFFF]*$").asPredicate();
    /**
     * A regex to validate resource type
     */
    static final Predicate<String> patternResourceType = compile("^[a-zA-Z]*$").asPredicate();
    /**
     * A regex to validate resource id
     */
    static final Predicate<String> patternResourceId = compile("^[a-zA-Z0-9-]*$").asPredicate();
    /**
     * A link to the official website
     */
    private static final String FHIR_WEBSITE_LINK = " See https://www.hl7.org/fhir/datatypes.html for allowed formats";
    private static final int MAX_TOKEN_LENGTH = 1024;

    private DataValidationUtils() {
    }

    /**
     * Validate a decimal. If the value is not valid, will throw an exception
     *
     * @param value the value to check
     * @throws BadDataFormatException if the value is not valid
     */
    public static void validateDecimal(String value) throws BadDataFormatException {
        assertNotNull(value);
        if (!patternDecimal.test(value)) {
            throw new BadDataFormatException("Bad decimal." + FHIR_WEBSITE_LINK);
        }
    }

    public static void validateDate(String value) throws BadDataFormatException {
        assertNotNull(value);
        if (!patternDateTime.test(value)) {
            throw new BadDataFormatException("Bad date format." + FHIR_WEBSITE_LINK);
        }
    }

    /**
     * Validate a String. If the value is not valid, will throw an exception
     *
     * @param value the value to check
     * @throws BadDataFormatException if the value is not valid
     */
    public static void validateString(String value) throws BadDataFormatException {
        assertNotNull(value);
        if (value.isEmpty()) {
            throw new BadDataFormatException("String should contain non-whitespace content.");
        }
        if (value.length() > 1_048_576) {
            throw new BadDataFormatException("String length shall not exceed 1 048 576 chars.");
        }

        if (!patternString.test(value)) {
            throw new BadDataFormatException("String only support FHIR chars." + FHIR_WEBSITE_LINK);
        }

    }

    /**
     * Validate a decimal. If the value is not valid, will throw an exception
     *
     * @param value the value to check
     * @throws BadDataFormatException if the value is not valid
     */
    public static void validateCanonical(String value) throws BadDataFormatException {
        assertNotNull(value);
        if (!patternCanonical.test(value)) {
            throw new BadDataFormatException("Bad canonical value");
        }
    }

    /**
     * Validate a code. If the value is not valid, will throw an exception
     *
     * @param value the value to check
     * @throws BadDataFormatException if the value is not valid
     */
    public static void validateCode(String value) throws BadDataFormatException {
        assertNotNull(value);
        if (!patternCode.test(value)) {
            throw new BadDataFormatException("Bad code value." + FHIR_WEBSITE_LINK);
        }
    }

    /**
     * Validate a parameter. If the value is not valid, will throw an exception
     *
     * @param value the value to check
     * @throws BadDataFormatException if the value is not valid
     */
    public static void validateTokenParameter(String value) throws BadDataFormatException {
        if (value != null && (MAX_TOKEN_LENGTH <= value.length() || !patternParameters.test(value))) {
            throw new BadDataFormatException("Bad parameter value.");
        }
    }

    /**
     * Validate a boolean. If the value is not valid, will throw an exception
     *
     * @param value the value to check
     * @throws BadDataFormatException if the value is not valid
     */
    public static void validateBoolean(String value) throws BadDataFormatException {
        assertNotNull(value);
        if (!value.equals("true") && !value.equals("false")) {
            throw new BadDataFormatException("Boolean must be true or false");
        }
    }

    /**
     * Throw an exception if the input is null
     *
     * @param value the value to test
     * @throws BadDataFormatException if the value is null
     */
    protected static void assertNotNull(String value) throws BadDataFormatException {
        if (value == null) {
            throw new BadDataFormatException("Value should not be null." + FHIR_WEBSITE_LINK);
        }
    }

    /**
     * Validate that include is valid. If not, will throw an exception
     *
     * @param resourceType the type of include
     * @param paramId      the id of resource to include
     */
    public static void validateIncludeParameter(String resourceType, String paramId) throws BadDataFormatException {
        // validate the resource type:
        if (resourceType != null && (MAX_TOKEN_LENGTH <= resourceType.length() || !patternResourceType.test(resourceType))) {
            throw new BadDataFormatException("Bad include parameter.");
        }
        // validate the resource id:
        assertNotNull(paramId);
        if (!"*".equals(paramId) && (MAX_TOKEN_LENGTH <= paramId.length() || !patternResourceId.test(paramId))) {
            throw new BadDataFormatException("Bad include parameter.");
        }
    }
}
