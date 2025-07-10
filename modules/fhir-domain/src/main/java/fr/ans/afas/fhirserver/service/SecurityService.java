/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.service;

import fr.ans.afas.exception.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;

public interface SecurityService {

    void canWriteResource(HttpServletRequest request) throws ForbiddenException;
}
