/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.sample;

import fr.ans.afas.AfasServerConfigurerAdapter;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.servlet.FhirServlet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * A sample application that expose a FHIR device endpoint with 2 search parameters.
 * <p>
 * 1) Supported resources and parameters:
 * <p>
 * The configuration of parameters can be found in the indexes.yml file.
 * <p>
 * The system support 2 types of configuration yaml of java. The actual configuration system is yaml.
 * <p>
 * If you want support java configuration, add a bean of type {@link SearchConfig} :
 *
 * <code>
 *
 * @author Guillaume Poulériguen
 * @Bean SearchConfig fhirResourceConfig() {
 * var config = new HashMap<String, List<SearchParamConfig>>();
 * config.put("Device", List.of(
 * SearchParamConfig.builder().name(Device.SP_DEVICE_NAME).urlParameter(Device.SP_DEVICE_NAME).resourcePaths(List.of(ResourcePathConfig.builder().path("deviceName|name").build())).indexName(StorageConstants.INDEX_DEVICE_NAME).searchType(StorageConstants.INDEX_TYPE_STRING).build(),
 * SearchParamConfig.builder().name(Device.SP_IDENTIFIER).urlParameter(Device.SP_IDENTIFIER).resourcePaths(List.of(ResourcePathConfig.builder().path("identifier").build())).indexName(StorageConstants.INDEX_DEVICE_IDENTIFIER).searchType(StorageConstants.INDEX_TYPE_TOKEN).build()
 * ));
 * return new BaseSearchConfigService(config) {
 * };
 * }
 * </code>
 * <p>
 * 2) Server enhancement
 * <p>
 * You can extend {@link AfasServerConfigurerAdapter} like this sample to configure some server information like the Hapi servlet.
 * @since 1.0.0
 */
@SpringBootApplication
public class FhirServerApplication extends AfasServerConfigurerAdapter {

    /**
     * Launch the service
     *
     * @param args command line args (not used)
     */
    public static void main(String[] args) {
        SpringApplication.run(FhirServerApplication.class, args);
    }


    /**
     * Configure the servlet
     *
     * @param fhirServlet the servlet to configure
     */
    @Override
    public void configureHapiServlet(FhirServlet fhirServlet) {
        fhirServlet.setServerName("My FHIR server");
    }
}
