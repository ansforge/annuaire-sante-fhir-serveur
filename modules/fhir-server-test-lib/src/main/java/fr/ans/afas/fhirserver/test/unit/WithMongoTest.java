/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.test.unit;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.util.StringUtils;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility class to use mongodb in tests. Use docker to launch a MongoDb instance.
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public final class WithMongoTest {

    /**
     * The mongodb container
     */
    static final AtomicReference<MongoDBContainer> mongoDBContainer = new AtomicReference<>();
    /**
     * Logger
     */
    private static final Logger logger = LoggerFactory.getLogger(WithMongoTest.class);

    private WithMongoTest() {
    }


    /**
     * Clean test context
     */
    public static void clean() {
        if (mongoDBContainer.get() != null) {
            mongoDBContainer.get().stop();
        }
    }

    /**
     * Configure the spring context
     */
    public static class PropertyOverrideContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {

            var mongoDockerEnabled = Boolean.parseBoolean(configurableApplicationContext.getEnvironment().getProperty("afas.mongodb.docker.enabled"));


            if (mongoDockerEnabled) {
                // if you encounter some problems with docker, disable RYUK with this env var:  TESTCONTAINERS_RYUK_DISABLED=true

                try {
                    var dockerRegistryUrl = configurableApplicationContext.getEnvironment().getProperty("docker.registry.url.mongodb");
                    mongoDBContainer.set(new MongoDBContainer(DockerImageName.parse(dockerRegistryUrl != null ? dockerRegistryUrl : "mongo:5.0")
                            .asCompatibleSubstituteFor("mongo")).withReuse(true));
                    logger.info("START Mongo");
                    mongoDBContainer.get().start();
                    logger.info("STARTED Mongo");
                    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(configurableApplicationContext, "afas.mongodb.uri=mongodb://"+ mongoDBContainer.get().getHost() + ":" + mongoDBContainer.get().getMappedPort(27017));
                } catch (Exception e) {
                    // no docker
                    logger.warn("No docker env found. Please run a local mongodb on port 27017.", e);
                    createLocalhostMongoInstance(configurableApplicationContext);

                }
            } else {
                clean();
                createLocalhostMongoInstance(configurableApplicationContext);
            }

        }

        private void createLocalhostMongoInstance(ConfigurableApplicationContext configurableApplicationContext) {
            var connectionString = configurableApplicationContext.getEnvironment().getProperty("test-mongodb-connection-string");
            if (!StringUtils.hasLength(connectionString)) {
                connectionString = "mongodb://localhost:27017";
            }
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    configurableApplicationContext, "afas.mongodb.uri=" + connectionString);
        }

    }


}