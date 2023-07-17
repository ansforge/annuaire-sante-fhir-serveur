/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.rass.service.impl;

import fr.ans.afas.rass.service.DatabaseService;

/**
 * Service to get the current database
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class SimpleDatabaseService implements DatabaseService {

    private final String name;

    public SimpleDatabaseService(String name) {
        this.name = name;
    }

    @Override
    public String getDatabase() {
        return name;
    }
}
