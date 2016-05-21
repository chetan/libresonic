package org.libresonic.player.util;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.Booleans;
import org.apache.logging.log4j.core.util.Integers;

public class HelpUtil {

    @Plugin(name="ListAppender", category="Core", elementType="appender", printObject=true)
    static class ListAppender extends AbstractAppender {

        private final BoundedList<String> events;

        private ListAppender(int listSize, String name, Filter filter, Layout<? extends Serializable> layout,
                boolean ignoreExceptions) {
            super(name, filter, layout, ignoreExceptions);
            this.events = new BoundedList<>(listSize);
        }

        @Override
        public void append(LogEvent event) {
            if (!isFiltered(event)) {
                events.add(new String(getLayout().toByteArray(event)));
            }
        }

        public BoundedList<String> getEvents() {
            return events;
        }

        @PluginFactory
        public static ListAppender createAppender(
                @PluginAttribute("size") final String size,
                @PluginAttribute("name") String name,
                @PluginElement("Layout") Layout<? extends Serializable> layout,
                @PluginElement("Filter") final Filter filter,
                @PluginAttribute("ignoreExceptions") final String ignore) {

            if (name == null) {
                LOGGER.error("No name provided for ListAppender");
                return null;
            }

            if (layout == null) {
                layout = PatternLayout.createDefaultLayout();
            }

            final int listSize = Integers.parseInt(size, 50);
            final boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);

            return new ListAppender(listSize, name, filter, layout, ignoreExceptions);
        }

    }

    private static Appender getAppender(String name) {
        Logger rootLogger = (Logger) LogManager.getRootLogger();
        Map<String, Appender> appenders = rootLogger.getAppenders();
        return appenders.get(name);
    }

    public static String getLogFilename() {
        RollingFileAppender appender = (RollingFileAppender) getAppender("RollingFile");
        return appender.getManager().getFileName();
    }

    public static List<String> getRecentLogs() {
        return ((ListAppender) getAppender("ListAppender")).getEvents();
    }

}
