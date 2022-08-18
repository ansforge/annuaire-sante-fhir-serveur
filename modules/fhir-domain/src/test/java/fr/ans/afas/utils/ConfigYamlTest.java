package fr.ans.afas.utils;

import fr.ans.afas.fhirserver.search.config.yaml.YamlSearchConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test the property service for yaml configuration
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class ConfigYamlTest {

    @Autowired
    private YamlSearchConfig yamlSearchConfig;

    /**
     * Test the nominal case
     */
    @Test
    public void testConfigOnly() {
        var patientConfig = yamlSearchConfig.getResources().get(0);
        Assert.assertEquals("Patient", patientConfig.getName());
        Assert.assertEquals("http://hl7.org/fhir/StructureDefinition/Patient", patientConfig.getProfile());
        var orgP1 = patientConfig.getSearchParams().get(0);
        Assert.assertEquals("active", orgP1.getName());
        Assert.assertEquals("active", orgP1.getUrlParameter());
        Assert.assertEquals("token", orgP1.getSearchType());
        Assert.assertEquals("Whether the patient record is active", orgP1.getDescription());
        Assert.assertEquals("t_active", orgP1.getIndexName());
        Assert.assertEquals(1, orgP1.getResourcePaths().size());
        var rp = orgP1.getResourcePaths();
        Assert.assertEquals("active", rp.get(0).getPath());
        Assert.assertEquals("address.city", patientConfig.getSearchParams().get(1).getResourcePaths().get(1).getPath());
    }


}
