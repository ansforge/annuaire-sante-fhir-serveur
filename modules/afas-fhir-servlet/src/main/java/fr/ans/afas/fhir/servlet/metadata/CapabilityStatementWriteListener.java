/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet.metadata;

import fr.ans.afas.fhir.servlet.error.ErrorWriter;
import fr.ans.afas.fhir.servlet.servletutils.DefaultWriteListener;
import fr.ans.afas.fhirserver.search.config.domain.FhirResourceSearchConfig;
import fr.ans.afas.fhirserver.service.FhirServerContext;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Enumerations;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Write the capability statement
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Slf4j
public class CapabilityStatementWriteListener<T> extends DefaultWriteListener {

    private final FhirServerContext<T> fhirServerContext;
    /**
     * The servlet output stream
     */
    private final ServletOutputStream sos;


    public CapabilityStatementWriteListener(FhirServerContext<T> fhirServerContext, ServletOutputStream sos, AsyncContext context) {
        super(context);
        this.sos = sos;
        this.fhirServerContext = fhirServerContext;
    }

    /**
     * Add interactions to the capability statement for a resource
     *
     * @param config   the config
     * @param resource the capability statement component
     */
    public static void addInteractions(FhirResourceSearchConfig config, CapabilityStatement.CapabilityStatementRestResourceComponent resource) {

        if (config.isCanRead()) {
            resource.addInteraction().setCode(CapabilityStatement.TypeRestfulInteraction.SEARCHTYPE);
            resource.addInteraction().setCode(CapabilityStatement.TypeRestfulInteraction.READ);
        }

    }

    @Override
    public void onWritePossibleInTenant() throws IOException {

            var cs = new CapabilityStatement();
            writeMeta(cs);
            cs.setRest(buildServer());
            sos.write(this.fhirServerContext.getFhirContext().newJsonParser().encodeResourceToString(cs).getBytes(Charset.defaultCharset()));
            context.complete();

    }

    /**
     * Write the capability statement metadata
     *
     * @param cs the capability statement
     */
    private void writeMeta(CapabilityStatement cs) {
        var searchConfigService = this.fhirServerContext.getSearchConfigService();
        if (StringUtils.isNotBlank(searchConfigService.getServerSearchConfig().getCopyright())) {
            cs.setCopyright(searchConfigService.getServerSearchConfig().getCopyright());
        }
        if (StringUtils.isNotBlank(searchConfigService.getServerSearchConfig().getImplementationGuideUrl())) {
            cs.setImplementationGuide(List.of(new CanonicalType(searchConfigService.getServerSearchConfig().getImplementationGuideUrl())));
        }
        cs.setDate(new Date());
        cs.setStatus(Enumerations.PublicationStatus.ACTIVE);
        cs.setFhirVersion(Enumerations.FHIRVersion._4_0_1);
        cs.setKind(CapabilityStatement.CapabilityStatementKind.INSTANCE);

        // Add implementation details
        cs.getImplementation()
                .setDescription("Spécification des capacités de l'API FHIR Annuaire Santé V2")
                .setUrl("https://gateway.api.esante.gouv.fr/fhir/v2");


        cs.setFormat(List.of(new CodeType("application/fhir+json"), new CodeType("json")));
    }

    @Override
    public void onError(Throwable throwable) {
        log.debug("Error writing the request", throwable);
        ErrorWriter.writeError("Unexpected error", context, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        context.complete();
    }

    /**
     * Build the server/rest section of the capability statement
     *
     * @return rest components of the capability statement
     */
    protected List<CapabilityStatement.CapabilityStatementRestComponent> buildServer() {
        var serverComponents = new ArrayList<CapabilityStatement.CapabilityStatementRestComponent>();
        var serverComponent = new CapabilityStatement.CapabilityStatementRestComponent();

        // add mode (SERVER)
        serverComponent.setMode(CapabilityStatement.RestfulCapabilityMode.SERVER);

        serverComponents.add(serverComponent);
        for (var config : this.fhirServerContext.getSearchConfigService().getServerSearchConfig().getResources()) {
            var resource = serverComponent.addResource();
            resource.setProfile(config.getProfile());
            resource.setSupportedProfile(config.getSupportedProfiles());
            resource.setType(config.getName());

            addInteractions(config, resource);

            resource.setSearchInclude(config.getSearchIncludes());
            resource.setSearchRevInclude(config.getSearchRevIncludes());

            for (var p : config.getSearchParams()) {
                var sp = resource.addSearchParam();
                sp.setName(p.getName());
                sp.setType(Enumerations.SearchParamType.fromCode(p.getSearchType()));
                sp.setDocumentation(p.getDescription());
                sp.setDefinition(p.getDefinition());
            }
        }
        return serverComponents;
    }


}
