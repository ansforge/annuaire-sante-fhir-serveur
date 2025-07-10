/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.config;

import fr.ans.afas.fhirserver.search.config.domain.ServerSearchConfig;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import fr.ans.afas.rass.service.MongoDbFhirService;
import fr.ans.afas.utils.TenantUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Inject;
import java.util.Date;

/**
 * The configuration of the job that clean revision data no more used.
 * The service will clean paging data too.
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@EnableScheduling
public class CleanRevisionDataConfiguration {


    /**
     * The storage service
     */
    @Inject
    MongoDbFhirService mongoDbFhirService;

    /**
     * The url manager
     */
    @Inject
    NextUrlManager<?> nextUrlManager;
    @Value("${afas.fhir.max-revision-duration}")
    long validityMs;
    @Inject
    private ServerSearchConfig serverSearchConfig;

    /**
     * Clean revisions older than a timestamp
     */
    @Scheduled(fixedDelay = 3600000)
    public void cleanOldRevision() {
        serverSearchConfig.getConfigs().forEach((key, value) -> {
            TenantUtil.setCurrentTenant(key);
            mongoDbFhirService.deleteOldRevisions(new Date().getTime() - validityMs);
            nextUrlManager.cleanOldPagingData(new Date().getTime() - validityMs);
        });
    }
}
