package fr.ans.afas.utils.audit;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class LoggerRule implements TestRule {

    private final ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    private final Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                setup();
                base.evaluate();
                teardown();
            }
        };
    }

    private void setup() {
        logger.addAppender(listAppender);
        listAppender.start();
    }

    private void teardown() {
        listAppender.stop();
        listAppender.list.clear();
        logger.detachAppender(listAppender);
    }

    public List<String> getMessages() {
        return listAppender.list.stream().map(ILoggingEvent::getMessage).collect(Collectors.toList());
    }

    public List<String> getFormattedMessages() {
        return listAppender.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.toList());
    }

}