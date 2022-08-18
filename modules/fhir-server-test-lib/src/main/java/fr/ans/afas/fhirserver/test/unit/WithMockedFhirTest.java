/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.test.unit;

import org.junit.Before;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;


/**
 * Utility class to use a mocked IRIS Da api. This test launch a nginx server with some FHIR bundles.
 * It is not an FHIR server. It is simply an http server, some urls of which are similar to an FHIR server.
 * The resources returned will always be the same regardless of the parameters.
 * <p>
 * Ex: When you call http://instance/Organization, you will always get the same file.
 * <p>
 * Bundles served by the server are placed under the folder "resources/docker/mocked-da" of this module.
 * <p>
 * This test automatically set the good configuration's properties in the spring context.
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class WithMockedFhirTest {

    /**
     * A Nginx container with static files to simulate IRIS DA
     */
    public static GenericContainer<?> mockedFhirContainer;

    /**
     * true if test data are init
     */
    protected static boolean setup;

    /**
     * Clean test context
     */
    public static void clean() {
        if (mockedFhirContainer != null) {
            mockedFhirContainer.stop();
        }
    }

    /**
     * Setup test context
     */
    @Before
    public void init() {
        if (!setup) {
            setup = true;
        }
    }


    /**
     * Configure the spring context
     */
    public static class PropertyOverrideContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {


            var dockerRegistryUrl = configurableApplicationContext.getEnvironment().getProperty("docker.registry.url.nginx");
            mockedFhirContainer = new GenericContainer<>(DockerImageName.parse(dockerRegistryUrl != null ? dockerRegistryUrl : "nginx:1.21.4"))
                    .withClasspathResourceMapping("docker/nginx-mock-fhir.conf",
                            "/etc/nginx/nginx.conf",
                            BindMode.READ_ONLY)
                    .withClasspathResourceMapping("docker/mocked-da",
                            "/usr/share/nginx/html/",
                            BindMode.READ_ONLY)
                    .withExposedPorts(80);

            // if you encounter some problems with docker, disable RYUK with this env var:  TESTCONTAINERS_RYUK_DISABLED=true
            setup = false;
            mockedFhirContainer.start();
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    configurableApplicationContext, "afas.fhir-import-data.daApiUrl=http://localhost:" + mockedFhirContainer.getMappedPort(80));
        }
    }


}
