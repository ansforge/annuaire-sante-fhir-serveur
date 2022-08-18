/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas;


import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * Configure mongodb client
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class MongoConfig {
    /**
     * Uri of the mongodb server
     */
    @Value("${afas.mongodb.uri}")
    String mongoUri;

    /**
     * Create a mongodb client
     *
     * @return the client
     */
    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create(mongoUri);
    }


}
