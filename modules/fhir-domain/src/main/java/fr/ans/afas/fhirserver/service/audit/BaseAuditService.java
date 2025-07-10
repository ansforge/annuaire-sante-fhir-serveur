/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.service.audit;

import fr.ans.afas.audit.AuditUtils;
import fr.ans.afas.fhirserver.hook.event.AfasEvent;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.IdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Nullable;

public class BaseAuditService {

    /**
     * Logger
     */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    /**
     * Version of the application
     */
    @Value("${project.version:-}")
    String version;

    protected void logRequest(AfasEvent event, @Nullable Object request, @Nullable Object response) {
        var fm = formatObject(request);
        var clientIp = AuditUtils.get().getIp();
        if (response == null) {
            if (logger.isInfoEnabled()) {
                logger.info("Event:{}\tClientIp:{}\tAppVersion:{}\tEvent:{}\tRequest:{}", event.getClass().getSimpleName(), clientIp, version, event.getClass().getSimpleName(), fm);
            }
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("Event:{}\tClientIp:{}\tAppVersion:{}\tEvent:{}\tRequest:{}\tResult:{}", event.getClass().getSimpleName(), clientIp, version, event.getClass().getSimpleName(), fm, response);
            }
        }
    }

    protected String formatObject(Object o) {
        if (o == null) {
            return "null";
        } else if (o instanceof DomainResource dr) {
            return dr.getResourceType() + "/" + dr.getId();
        } else if (o instanceof IdType drId) {
            return drId.toString();
        } else {
            return o.toString();
        }
    }
}
