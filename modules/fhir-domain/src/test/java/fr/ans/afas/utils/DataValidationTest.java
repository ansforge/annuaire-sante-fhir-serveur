/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.utils;

import fr.ans.afas.exception.BadDataFormatException;
import fr.ans.afas.validation.DataValidationUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.stream.IntStream;


public class DataValidationTest {

    @Test
    public void validateStringConstraints() throws BadDataFormatException {
        // null
        Assert.assertThrows(BadDataFormatException.class, () -> DataValidationUtils.validateString(null));

        // numbers
        DataValidationUtils.validateString("123");

        // special chars
        DataValidationUtils.validateString("è^%\\//");

        // utf8
        DataValidationUtils.validateString("\u009a\u0084");

        var invalid = "\u0010";
        Assert.assertThrows(BadDataFormatException.class, () -> DataValidationUtils.validateString(invalid));

        var validSpecialCharTab = "	";
        DataValidationUtils.validateString(validSpecialCharTab);

        var validSpecialCharCarriageReturn = "\r";
        DataValidationUtils.validateString(validSpecialCharCarriageReturn);

        var validSpecialChar13 = "\n";
        DataValidationUtils.validateString(validSpecialChar13);

        // Too long string
        var longString = IntStream.range(0, 1024 * 1024).mapToObj(b -> "a").collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
        DataValidationUtils.validateString(longString);
        Assert.assertThrows(BadDataFormatException.class, () -> DataValidationUtils.validateString(longString + "a"));

    }

    @Test
    public void validateDateConstraints() throws BadDataFormatException {

        DataValidationUtils.validateDate("2013");
        DataValidationUtils.validateDate("2013-05");
        DataValidationUtils.validateDate("2013-10-20");
        DataValidationUtils.validateDate("2015-02-07T13:28:17");
        DataValidationUtils.validateDate("2015-02-07T13:28:17-05:00");
        DataValidationUtils.validateDate("2015-02-07T13:28:00");

        Assert.assertThrows(BadDataFormatException.class, () -> DataValidationUtils.validateDate("2015-0a"));

        // null
        Assert.assertThrows(BadDataFormatException.class, () -> DataValidationUtils.validateDate(null));

    }

    @Test
    public void validateCodeConstraints() throws BadDataFormatException {
        DataValidationUtils.validateCode("some-code");
        Assert.assertThrows(BadDataFormatException.class, () -> DataValidationUtils.validateCode(null));
    }

    @Test
    public void validateDecimalConstraints() throws BadDataFormatException {
        DataValidationUtils.validateDecimal("100210");
        DataValidationUtils.validateDecimal("1.00210");
        Assert.assertThrows(BadDataFormatException.class, () -> DataValidationUtils.validateDecimal(null));
        Assert.assertThrows(BadDataFormatException.class, () -> DataValidationUtils.validateDecimal("\n"));
        Assert.assertThrows(BadDataFormatException.class, () -> DataValidationUtils.validateDecimal("_"));
        Assert.assertThrows(BadDataFormatException.class, () -> DataValidationUtils.validateDecimal("1-1"));
        Assert.assertThrows(BadDataFormatException.class, () -> DataValidationUtils.validateDecimal("12345678901234567890"));
        Assert.assertThrows(BadDataFormatException.class, () -> DataValidationUtils.validateDecimal("1,00210"));
    }

    @Test
    public void validateBooleanConstraints() throws BadDataFormatException {
        DataValidationUtils.validateBoolean("true");
        DataValidationUtils.validateBoolean("false");
        Assert.assertThrows(BadDataFormatException.class, () -> DataValidationUtils.validateBoolean(null));
        Assert.assertThrows(BadDataFormatException.class, () -> DataValidationUtils.validateBoolean("True"));
        Assert.assertThrows(BadDataFormatException.class, () -> DataValidationUtils.validateBoolean("truee"));

    }

    @Test
    public void validateCanonicalConstraints() throws BadDataFormatException {
        DataValidationUtils.validateCanonical("some");
        Assert.assertThrows(BadDataFormatException.class, () -> DataValidationUtils.validateCanonical(null));
    }

    @Test
    public void validateIncludeConstraints() throws BadDataFormatException {
        DataValidationUtils.validateIncludeParameter("Practitioner", "123");
        DataValidationUtils.validateIncludeParameter(null, "*");
        Assert.assertThrows(BadDataFormatException.class, () -> DataValidationUtils.validateIncludeParameter("Practitioner", "_"));
        Assert.assertThrows(BadDataFormatException.class, () -> DataValidationUtils.validateIncludeParameter("Practitioner", null));
        Assert.assertThrows(BadDataFormatException.class, () -> DataValidationUtils.validateIncludeParameter("Pr", "44568A "));


        var longString = IntStream.range(0, 1024 * 1024).mapToObj(b -> "a").collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
        Assert.assertThrows(BadDataFormatException.class, () -> DataValidationUtils.validateIncludeParameter("Pr", longString));
        Assert.assertThrows(BadDataFormatException.class, () -> DataValidationUtils.validateIncludeParameter(longString, "ids"));

    }


}
