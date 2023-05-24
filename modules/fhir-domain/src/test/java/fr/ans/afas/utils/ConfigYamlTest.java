/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.utils;

import fr.ans.afas.fhirserver.search.config.yaml.YamlSearchConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;

/**
 * Test the property service for yaml configuration
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class ConfigYamlTest {

    @Inject
    private YamlSearchConfig yamlSearchConfig;

    /**
     * Test the nominal case
     */
    @Test
    public void testConfigOnly() {
        var patientConfig = yamlSearchConfig.getResources().iterator().next();
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
