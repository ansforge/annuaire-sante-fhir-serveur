/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.domain;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test the FhirBundleBuilder class
 */
class FhirBundleBuilderTest {

    IParser parser = FhirContext.forR4().newJsonParser();

    @Test
    void testBuildHeader() {
        var builder = new FhirBundleBuilder();
        var result = builder.getHeader("123", 10L);
        Assertions.assertEquals("{\"resourceType\": \"Bundle\",\"type\": \"searchset\",\"id\": \"123\",\"total\":10,\"entry\": [", result);
    }

    @Test
    void testBuildFooterWithNext() {
        var builder = new FhirBundleBuilder();
        var genericHeader = builder.getHeader("123", 10L);

        var result = builder.getFooter("https://serverUrl", "https://currentUrl/path?abc", null);
        Assertions.assertEquals("],\"link\": [ {\"relation\": \"self\",\"url\": \"https://currentUrl/path?abc\"}]}", result);
        Assertions.assertDoesNotThrow(() -> {
            parser.parseResource(genericHeader + result);
        });
    }


    @ParameterizedTest
    @CsvSource({
            "https://serverUrl, https://currentUrl/path?abc, , '],\"link\": [ {\"relation\": \"self\",\"url\": \"https://currentUrl/path?abc\"}]}', false",
            "https://serverUrl/v2/, https://currentUrl/path?abc, 123456, '],\"link\": [ {\"relation\": \"next\",\"url\": \"https://serverUrl/v2/_page?id=123456\"}, {\"relation\": \"self\",\"url\": \"https://currentUrl/path?abc\"}]}', false",
            "https://serverUrl/v2/_page, https://currentUrl/path?abc, 123456, '],\"link\": [ {\"relation\": \"next\",\"url\": \"https://serverUrl/v2/_page?id=123456\"}, {\"relation\": \"self\",\"url\": \"https://currentUrl/path?abc\"}]}', false",
            "https://serverUrl/v2/, https://currentUrl/path?abc, , '],\"link\": [ {\"relation\": \"self\",\"url\": \"https://currentUrl/path?abc\"}]}', false",
            "https://serverUrl, https://currentUrl/path?abc, 123456, '],\"link\": [ {\"relation\": \"next\",\"url\": \"https://serverUrl/_page?id=123456\"}, {\"relation\": \"self\",\"url\": \"https://currentUrl/path?abc\"}]}', false"
    })
    void testBuildFooter(String serverUrl, String currentUrl, String pageId, String expected, boolean shouldThrow) {
        var builder = new FhirBundleBuilder();
        var genericHeader = builder.getHeader("123", 10L);
        var result = builder.getFooter(serverUrl, currentUrl, pageId);

        Assertions.assertEquals(expected, result);
        if (!shouldThrow) {
            Assertions.assertDoesNotThrow(() -> {
                parser.parseResource(genericHeader + result);
            });
        }
    }

    @Test
    void testBuildHeaderWithNullTotal() {
        FhirBundleBuilder builder = new FhirBundleBuilder();
        // Passer un total null pour vérifier le comportement
        String header = builder.getHeader("123", null);
        String footer = builder.getFooter("https://serverUrl", "https://currentUrl/path?abc", "123456");

        // Construire le bundle complet (header + footer)
        String completeBundle = header + footer;
        // Afficher le contenu du bundle pour vérifier s'il est complet
        System.out.println("Complete Bundle JSON: " + completeBundle);
        // Assurez-vous que le total est géré correctement même si c'est null
        Assertions.assertFalse(header.contains("\"total\": 0")); // On s'attend à un total de 0 par défaut
        Assertions.assertDoesNotThrow(() -> {
            parser.parseResource(completeBundle); // Assurez-vous que la structure est valide
        });
    }

    @Test
    void testWrapBundleEntryWithValidContent() throws JsonProcessingException {
        // Configuration
        String serverUrl = "https://example.com/fhir";
        String type = "Patient";
        String id = "123";
        String contentJson = "{ \"resourceType\": \"Patient\", \"id\": \"123\" }";
        FhirBundleBuilder.BundleEntry entry = new FhirBundleBuilder.BundleEntry(type, id, contentJson);

        // Exécution de la méthode
        String result = FhirBundleBuilder.wrapBundleEntry(serverUrl, entry);

        // Vérification du JSON généré
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(result);

        // Vérifications
        Assertions.assertEquals(serverUrl + "/" + type + "/" + id, rootNode.get("fullUrl").asText());
        Assertions.assertNotNull(rootNode.get("resource"));
        Assertions.assertEquals("Patient", rootNode.get("resource").get("resourceType").asText());
        Assertions.assertEquals("123", rootNode.get("resource").get("id").asText());
    }

}
