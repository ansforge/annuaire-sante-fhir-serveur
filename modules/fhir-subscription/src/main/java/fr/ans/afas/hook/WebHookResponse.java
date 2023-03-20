/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */
package fr.ans.afas.hook;

import lombok.Getter;

import java.util.Date;

@Getter
public class WebHookResponse {

    private final boolean success;

    private final int statusCode;

    private final Date date;

    private final String log;

    public WebHookResponse(boolean success, int statusCode, Date date, String log) {
        this.success = success;
        this.statusCode = statusCode;
        this.date = date;
        this.log = log;
    }
}
