/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet.servletutils;


import jakarta.servlet.AsyncContext;
import jakarta.servlet.WriteListener;

import java.io.IOException;


public abstract class DefaultWriteListener extends BaseServletListener implements WriteListener {

    protected DefaultWriteListener(AsyncContext context) {
        super(context);
    }


    @Override
    public final void onWritePossible() throws IOException {
        setTenant();
        onWritePossibleInTenant();
    }

    public abstract void onWritePossibleInTenant() throws IOException;
}
