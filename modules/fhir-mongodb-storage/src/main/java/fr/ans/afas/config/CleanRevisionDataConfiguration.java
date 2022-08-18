/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.config;

import fr.ans.afas.rass.service.MongoDbFhirService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Date;

/**
 * The configuration of the job that clean revision data no more used
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@EnableScheduling
public class CleanRevisionDataConfiguration {


    /**
     * The storage service
     */
    @Autowired
    MongoDbFhirService mongoDbFhirService;

    @Value("${afas.fhir.max-revision-duration}")
    long validityMs;

    /**
     * Clean revisions older than a timestamp
     */
    @Scheduled(fixedDelay = 3600000)
    public void cleanOldRevision() {
        mongoDbFhirService.deleteOldRevisions(new Date().getTime() - validityMs);
    }


}
