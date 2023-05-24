/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir.servlet.metadata;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.config.domain.FhirResourceSearchConfig;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Enumerations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Write the capability statement
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class CapabilityStatementWriteListener implements WriteListener {
    /**
     * Logger
     */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    /**
     * The fhir parser
     */
    final IParser parser;

    /**
     * The servlet output stream
     */
    final ServletOutputStream sos;

    /**
     * The async context
     */
    final AsyncContext context;

    /**
     * The search config
     */
    final SearchConfig searchConfig;

    public CapabilityStatementWriteListener(ServletOutputStream sos, AsyncContext context, FhirContext fhirContext, SearchConfig searchConfig) {
        this.parser = fhirContext.newJsonParser();
        this.sos = sos;
        this.context = context;
        this.searchConfig = searchConfig;
    }

    /**
     * Add interactions to the capability statement for a resource
     *
     * @param config   the config
     * @param resource the capability statement component
     */
    private static void addInteractions(FhirResourceSearchConfig config, CapabilityStatement.CapabilityStatementRestResourceComponent resource) {
        if (config.isCanDelete()) {
            resource.addInteraction().setCode(CapabilityStatement.TypeRestfulInteraction.DELETE);
        }
        if (config.isCanRead()) {
            resource.addInteraction().setCode(CapabilityStatement.TypeRestfulInteraction.SEARCHTYPE);
            resource.addInteraction().setCode(CapabilityStatement.TypeRestfulInteraction.READ);
        }
        if (config.isCanWrite()) {
            resource.addInteraction().setCode(CapabilityStatement.TypeRestfulInteraction.CREATE);
            resource.addInteraction().setCode(CapabilityStatement.TypeRestfulInteraction.UPDATE);
        }
    }

    @Override
    public void onWritePossible() {
        try {
            var cs = new CapabilityStatement();
            writeMeta(cs);
            cs.setRest(buildServer());
            sos.write(parser.encodeResourceToString(cs).getBytes(Charset.defaultCharset()));
            context.complete();
        } catch (Exception e) {
            logger.debug("Error writing the request", e);
            context.complete();
        }
    }

    /**
     * Write the capability statement metadata
     *
     * @param cs the capability statement
     */
    private void writeMeta(CapabilityStatement cs) {
        if (StringUtils.isNotBlank(searchConfig.getServerSearchConfig().getCopyright())) {
            cs.setCopyright(searchConfig.getServerSearchConfig().getCopyright());
        }
        if (StringUtils.isNotBlank(searchConfig.getServerSearchConfig().getImplementationGuideUrl())) {
            cs.setImplementationGuide(List.of(new CanonicalType(searchConfig.getServerSearchConfig().getImplementationGuideUrl())));
        }
        cs.setStatus(Enumerations.PublicationStatus.ACTIVE);
        cs.setFhirVersion(Enumerations.FHIRVersion._4_0_1);
        // we only support json:
        cs.setFormat(List.of(new CodeType("application/fhir+json"), new CodeType("json")));
    }

    @Override
    public void onError(Throwable throwable) {
        logger.debug("Error writing the request", throwable);
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
        serverComponents.add(serverComponent);
        for (var config : searchConfig.getServerSearchConfig().getResources()) {
            var resource = serverComponent.addResource();
            resource.setProfile(config.getProfile());
            resource.setType(config.getName());

            addInteractions(config, resource);

            for (var p : config.getSearchParams()) {
                var sp = resource.addSearchParam();
                sp.setName(p.getName());
                sp.setType(Enumerations.SearchParamType.fromCode(p.getSearchType()));
                sp.setDocumentation(p.getDescription());
            }
        }
        return serverComponents;
    }


}
