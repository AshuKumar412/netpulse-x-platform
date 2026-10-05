package com.netpulse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.awt.Desktop;
import java.net.URI;

@SpringBootApplication
@EnableScheduling
public class NetPulseApplication {

    private static final Logger log = LoggerFactory.getLogger(NetPulseApplication.class);

    private final Environment environment;

    public NetPulseApplication(Environment environment) {
        this.environment = environment;
    }

    public static void main(String[] args) {
        SpringApplication.run(NetPulseApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String port = environment.getProperty("server.port", "8080");
        String url = "http://localhost:" + port;

        log.info("================================================================================");
        log.info("  NETPULSE X — Self-Healing Infrastructure Platform is READY");
        log.info("  Operations Console URL: {}", url);
        log.info("================================================================================");

        boolean isDesktopMode = Boolean.parseBoolean(System.getProperty("netpulse.desktop.mode",
                environment.getProperty("netpulse.desktop.mode", "false")));

        if (isDesktopMode && !java.awt.GraphicsEnvironment.isHeadless()) {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(url));
                    log.info("Opened NetPulse X Operations Console in default browser: {}", url);
                }
            } catch (Throwable t) {
                log.debug("Auto browser open skipped: {}", t.getMessage());
            }
        }
    }
}
