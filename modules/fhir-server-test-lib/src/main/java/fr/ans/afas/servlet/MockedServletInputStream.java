/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.servlet;

import org.springframework.util.Assert;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class MockedServletInputStream extends ServletInputStream {
    private final InputStream sourceStream;
    ReadListener readListener;
    private boolean finished = false;

    public MockedServletInputStream(InputStream sourceStream) {
        Assert.notNull(sourceStream, "Source InputStream must not be null");
        this.sourceStream = sourceStream;
    }


    public int read() throws IOException {
        int data = this.sourceStream.read();
        if (data == -1) {
            this.finished = true;
        }

        if (this.finished && readListener != null) {
            readListener.onAllDataRead();
        }
        return data;
    }

    @Override
    public int available() throws IOException {
        int available = this.sourceStream.available();
        if (available > 0 && readListener != null) {
            readListener.onDataAvailable();
        }
        return available;
    }

    @Override
    public void close() throws IOException {
        super.close();
        this.sourceStream.close();
    }

    public boolean isFinished() {
        return this.finished;
    }

    public boolean isReady() {
        return true;
    }

    public void setReadListener(ReadListener readListener) {
        this.readListener = readListener;
    }
}
