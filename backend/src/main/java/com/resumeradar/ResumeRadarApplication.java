package com.resumeradar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication
@Slf4j
public class ResumeRadarApplication {

    private final Environment env;

    public ResumeRadarApplication(Environment env) {
        this.env = env;
    }

    public static void main(String[] args) {
        SpringApplication.run(ResumeRadarApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String protocol = "http";
        String serverPort = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "");
        String hostAddress = "localhost";

        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("The host name could not be determined, using `localhost` as fallback");
        }

        System.out.println("\n----------------------------------------------------------");
        System.out.println("  ResumeRadar Application is running!");
        System.out.println("  Access URLs:");
        System.out.println("  Local:   " + protocol + "://localhost:" + serverPort + contextPath + "/");
        System.out.println("  Network: " + protocol + "://" + hostAddress + ":" + serverPort + contextPath + "/");
        System.out.println("----------------------------------------------------------\n");
    }
}
