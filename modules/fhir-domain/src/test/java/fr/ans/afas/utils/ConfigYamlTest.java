/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.utils;

import fr.ans.afas.fhirserver.search.config.yaml.MultiConfigLoader;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Test the property service for yaml configuration
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class ConfigYamlTest {


    /**
     * Test the loading of multiple configs
     */
    @Test
    public void testMultipleConfigs() throws URISyntaxException, IOException {

        var m = new MultiConfigLoader();
        var cs = m.loadConfigs("indexes/");

        Assertions.assertEquals(2, cs.size());

        var c1 = cs.get(0);
        Assertions.assertNull(c1.getCopyright());
        Assertions.assertEquals(1, c1.getResources().size());
        Assertions.assertEquals("Patient", c1.getResources().iterator().next().getName());

        var c2 = cs.get(1);
        Assertions.assertEquals("Device", c2.getResources().iterator().next().getName());
    }


}
