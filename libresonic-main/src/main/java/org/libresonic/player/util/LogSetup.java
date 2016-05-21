package org.libresonic.player.util;

import org.apache.logging.log4j.LogManager;
import org.libresonic.player.service.SettingsService;

public class LogSetup {
    public void init() {
        // make sure home is always set and reload log4j (if necessary)
        System.setProperty("libresonic.home", SettingsService.getLibresonicHome().getPath());
        ((org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false)).reconfigure();
    }
}