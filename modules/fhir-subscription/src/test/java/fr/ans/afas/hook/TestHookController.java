/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */
package fr.ans.afas.hook;


import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 *
 */
@RestController
@RequestMapping("hooks")
class TestHookController {

    private int nbCalled = 0;
    private int nbErrorCalled = 0;
    private boolean hasBody = false;

    @PostMapping()
    public void postHook(@RequestBody(required = false) String body) {
        this.nbCalled++;
        this.hasBody = Strings.isNotBlank(body);
    }

    @PostMapping("error")
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void postHookError() throws Exception {
        this.nbErrorCalled++;
        throw new Exception();
    }

    public int getNbCalled() {
        return nbCalled;
    }

    public int getNbErrorCalled() {
        return nbErrorCalled;
    }

    public boolean hasBody() {
        return hasBody;
    }

    public void resetData() {
        this.nbCalled = 0;
        this.nbErrorCalled = 0;
        this.hasBody = false;
    }
}