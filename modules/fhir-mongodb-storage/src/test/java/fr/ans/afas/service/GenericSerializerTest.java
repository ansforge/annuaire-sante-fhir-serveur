package fr.ans.afas.service;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import fr.ans.afas.domain.StorageConstants;
import fr.ans.afas.fhirserver.search.config.BaseSearchConfigService;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.config.domain.FhirResourceSearchConfig;
import fr.ans.afas.fhirserver.search.config.domain.ResourcePathConfig;
import fr.ans.afas.fhirserver.search.config.domain.SearchParamConfig;
import fr.ans.afas.fhirserver.search.config.domain.ServerSearchConfig;
import fr.ans.afas.rass.service.json.GenericSerializer;
import org.bson.Document;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Test the automatic serialization of objects
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class GenericSerializerTest {

    /**
     * The serializer to test
     */
    static GenericSerializer genericSerializer = new GenericSerializer(fhirResourceConfig(), FhirContext.forR4());


    /**
     * Register an object mapper to run our tests
     */
    static ObjectMapper om = new ObjectMapper();

    static {
        var module = new SimpleModule();
        module.addSerializer(Device.class, genericSerializer);
        om.registerModule(module);
    }

    static SearchConfig fhirResourceConfig() {
        var config = new HashMap<String, FhirResourceSearchConfig>();

        config.put("Device", FhirResourceSearchConfig.builder()
                .name("Device")
                .profile("http://hl7.org/fhir/StructureDefinition/Device")
                .searchParams(List.of(
                        SearchParamConfig.builder().name(Device.SP_DEVICE_NAME).urlParameter(Device.SP_DEVICE_NAME).resourcePaths(List.of(ResourcePathConfig.builder().path("deviceName|name").build())).indexName(StorageConstants.INDEX_DEVICE_NAME).searchType(StorageConstants.INDEX_TYPE_STRING).build(),
                        SearchParamConfig.builder().name(Device.SP_IDENTIFIER).urlParameter(Device.SP_IDENTIFIER).resourcePaths(List.of(ResourcePathConfig.builder().path("identifier").build())).indexName(StorageConstants.INDEX_DEVICE_IDENTIFIER).searchType(StorageConstants.INDEX_TYPE_TOKEN).build(),
                        SearchParamConfig.builder().name("multi-path").urlParameter("multi-path").resourcePaths(
                                        List.of(
                                                ResourcePathConfig.builder().path("deviceName|name").build(),
                                                ResourcePathConfig.builder().path("lotNumber").build())
                                )
                                .indexName("t_multi-path-index").searchType(StorageConstants.INDEX_TYPE_STRING).build(),
                        SearchParamConfig.builder().name("extension-sample").urlParameter("extension-sample").resourcePaths(List.of(ResourcePathConfig.builder().path("extension.?[#this.url=='someUrl']|value").build())).indexName("t_extension-sample").searchType(StorageConstants.INDEX_TYPE_TOKEN).build(),
                        SearchParamConfig.builder().name("extension-sample2").urlParameter("extension-sample2").resourcePaths(List.of(ResourcePathConfig.builder().path("extension.?[#this.url=='someUrl2']|value").build())).indexName("t_extension-sample2").searchType(StorageConstants.INDEX_TYPE_TOKEN).build(),
                        SearchParamConfig.builder().name("extension-sample3").urlParameter("extension-sample3").resourcePaths(List.of(ResourcePathConfig.builder().path("extension.?[#this.url=='withSub']|extension.?[#this.url=='sub']|value").build())).indexName("t_extension-sample3").searchType(StorageConstants.INDEX_TYPE_TOKEN).build(),
                        SearchParamConfig.builder().name("status").urlParameter("status").resourcePaths(List.of(ResourcePathConfig.builder().path("status?.toCode()").build())).indexName("t_status").searchType(StorageConstants.INDEX_TYPE_TOKEN).build()
                ))
                .build());

        return new BaseSearchConfigService(ServerSearchConfig.builder()
                .resources(config.values())
                .build()) {
        };
    }

    /**
     * Test the value extractor (values are extracted by path)
     */
    @Test
    public void testValueExtractor() {

        var genericSerializer = new GenericSerializer(null, null) {
            {
            }

            @Override
            public Collection<Object> extractValues(Object value, String stringPath) {
                return super.extractValues(value, stringPath);
            }
        };

        var device = generateDeviceWithArray();
        var result = genericSerializer.extractValues(device, "identifier");
        Assert.assertEquals(2, result.size());
        var i = result.stream().iterator();
        Assert.assertEquals(Identifier.class, i.next().getClass());
        Assert.assertEquals(Identifier.class, i.next().getClass());

        var result2 = genericSerializer.extractValues(device, "deviceName|name");
        Assert.assertEquals(2, result2.size());
        var i2 = result2.stream().iterator();
        Assert.assertEquals(String.class, i2.next().getClass());
        Assert.assertEquals(String.class, i2.next().getClass());
    }

    /**
     * Test the basic serialization
     */
    @Test
    public void entityDetectionTest() throws IOException {
        var device = generateDevice();
        var writer = om.writer();
        var json = writer.writeValueAsString(device);

        // assert that indexes are created:
        var doc = Document.parse(json);
        Assert.assertEquals("My device name", ((List<String>) doc.get("t_name")).get(0));
        Assert.assertEquals("http://system.org/", ((List<String>) doc.get("t_identifier-system")).get(0));
        Assert.assertEquals("ID-1", ((List<String>) doc.get("t_identifier-value")).get(0));
        Assert.assertEquals("http://system.org/|ID-1", ((List<String>) doc.get("t_identifier-sysval")).get(0));
    }

    /**
     * Test the serialization when there is arrays
     */
    @Test
    public void arraySerializationTest() throws JsonProcessingException {
        var device = generateDeviceWithArray();
        var writer = om.writer();
        var json = writer.writeValueAsString(device);

        // assert that indexes are created:
        var doc = Document.parse(json);
        Assert.assertArrayEquals(new String[]{"My device name2", "My device name3"}, ((List<String>) doc.get("t_name")).toArray());

        Assert.assertEquals("http://system.org/", ((List<String>) doc.get("t_identifier-system")).get(0));
        Assert.assertEquals("ID-1", ((List<String>) doc.get("t_identifier-value")).get(0));
        Assert.assertEquals("http://system.org/|ID-1", ((List<String>) doc.get("t_identifier-sysval")).get(0));


        Assert.assertEquals("http://system.org2/", ((List<String>) doc.get("t_identifier-system")).get(1));
        Assert.assertEquals("ID-2", ((List<String>) doc.get("t_identifier-value")).get(1));
        Assert.assertEquals("http://system.org2/|ID-2", ((List<String>) doc.get("t_identifier-sysval")).get(1));
    }

    /**
     * Test when the configuration contains multiple path for a search field
     */
    @Test
    public void multiPathTest() throws JsonProcessingException {
        var device = generateDeviceWithMultiplePaths();
        var writer = om.writer();
        var json = writer.writeValueAsString(device);

        // assert that indexes are created:
        var doc = Document.parse(json);

        Assert.assertEquals("My device name2", ((List<String>) doc.get("t_multi-path-index")).get(0));
        Assert.assertEquals("Lot 1", ((List<String>) doc.get("t_multi-path-index")).get(1));

    }

    @Test
    public void extensionSerializationTest() throws JsonProcessingException {

        var device = generateDeviceWithExtension();
        var writer = om.writer();
        var json = writer.writeValueAsString(device);

        // assert that indexes are created:
        var doc = Document.parse(json);
        // simple extension:
        Assert.assertEquals(1, ((List<String>) doc.get("t_extension-sample")).size());
        Assert.assertEquals("someValue", ((List<String>) doc.get("t_extension-sample")).get(0));

        // multiple extension:
        Assert.assertEquals(2, ((List<String>) doc.get("t_extension-sample2")).size());
        Assert.assertEquals("someValue2", ((List<String>) doc.get("t_extension-sample2")).get(0));
        Assert.assertEquals("someValue3", ((List<String>) doc.get("t_extension-sample2")).get(1));


        // multiple sub extensions:
        Assert.assertEquals(2, ((List<String>) doc.get("t_extension-sample3")).size());
        Assert.assertEquals("subValue1", ((List<String>) doc.get("t_extension-sample3")).get(0));
        Assert.assertEquals("subValue2", ((List<String>) doc.get("t_extension-sample3")).get(1));

    }

    @Test
    public void spelUsageInExtractionTest() throws JsonProcessingException {
        var device = generateDeviceWithStatus();
        var writer = om.writer();
        var json = writer.writeValueAsString(device);

        // assert that indexes are created:
        var doc = Document.parse(json);
        Assert.assertEquals(1, ((List<String>) doc.get("t_status-value")).size());
        Assert.assertEquals("active", ((List<String>) doc.get("t_status-value")).get(0));

    }


    /**
     * Generate a simple device
     */
    Device generateDevice() {
        Device device01 = new Device();
        device01.setId("id-1");
        device01.addDeviceName().setName("My device name");
        device01.addIdentifier().setSystem("http://system.org/").setValue("ID-1");
        return device01;
    }

    /**
     * Generate a simple device with a status (enumeration)
     */
    Device generateDeviceWithStatus() {
        Device device01 = new Device();
        device01.setId("id-1");
        device01.setStatus(Device.FHIRDeviceStatus.ACTIVE);
        return device01;
    }

    /**
     * Generate a device with multiple values for one field
     */
    Device generateDeviceWithArray() {
        Device device01 = new Device();
        device01.setId("id-2");
        device01.addDeviceName().setName("My device name2");
        device01.addDeviceName().setName("My device name3");
        device01.addIdentifier().setSystem("http://system.org/").setValue("ID-1");
        device01.addIdentifier().setSystem("http://system.org2/").setValue("ID-2");
        return device01;
    }


    /**
     * Generate a simple device that have multiple fields with the string type (deviceName and lotNumber)
     */
    Device generateDeviceWithMultiplePaths() {
        Device device01 = new Device();
        device01.setId("id-2");
        device01.addDeviceName().setName("My device name2");
        device01.setLotNumber("Lot 1");
        return device01;
    }

    /**
     * Generate a device with extensions
     */
    Device generateDeviceWithExtension() {
        Device device01 = new Device();
        device01.setId("id-2");
        device01.addExtension().setUrl("someUrl").setValue(new StringType("someValue"));
        device01.addExtension().setUrl("someUrl2").setValue(new StringType("someValue2"));
        device01.addExtension().setUrl("someUrl2").setValue(new StringType("someValue3"));

        device01.addExtension().setUrl("withSub")
                .addExtension().setUrl("sub").setValue(new StringType("subValue1"));
        device01.addExtension().setUrl("withSub")
                .addExtension().setUrl("ko").setValue(new StringType("subValueOK"));
        device01.addExtension().setUrl("withSub")
                .addExtension().setUrl("sub").setValue(new StringType("subValue2"));
        return device01;
    }


}
