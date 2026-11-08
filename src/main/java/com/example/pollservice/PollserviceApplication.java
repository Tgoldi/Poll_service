package com.example.pollservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PollserviceApplication {

	public static void main(String[] args) {
		disableH2ConsoleUnlessOptedIn();
		SpringApplication.run(PollserviceApplication.class, args);
	}

	/**
	 * The H2 web console gives full read/write access to the database (and can run code on the
	 * server), and the datasource has no password. Keep it off unless a developer explicitly opts
	 * in on their own machine with POLL_H2_CONSOLE_ENABLED=true. A Java system property overrides
	 * the value in application.properties.
	 */
	private static void disableH2ConsoleUnlessOptedIn() {
		String optIn = System.getenv("POLL_H2_CONSOLE_ENABLED");
		if (optIn == null || !optIn.trim().equalsIgnoreCase("true")) {
			System.setProperty("spring.h2.console.enabled", "false");
			System.err.println("WARNING: the H2 web console is disabled. Set POLL_H2_CONSOLE_ENABLED=true only for "
					+ "local development to turn it on.");
		} else {
			// Even when opted in, never let remote machines reach the console.
			System.setProperty("spring.h2.console.settings.web-allow-others", "false");
		}
	}

}
