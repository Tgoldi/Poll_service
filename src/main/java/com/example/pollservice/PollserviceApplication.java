package com.example.pollservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PollserviceApplication {

	private static final String H2_CONSOLE_PROPERTY = "spring.h2.console.enabled";

	public static void main(String[] args) {
		disableH2ConsoleUnlessExplicitlyEnabled(args);
		SpringApplication.run(PollserviceApplication.class, args);
	}

	/**
	 * The H2 web console gives full, unauthenticated access to the database.
	 * It stays off unless it is turned on outside the packaged configuration
	 * (environment variable SPRING_H2_CONSOLE_ENABLED=true, the JVM option
	 * -Dspring.h2.console.enabled=true, or the command-line argument
	 * --spring.h2.console.enabled=true), which should only be done locally.
	 */
	private static void disableH2ConsoleUnlessExplicitlyEnabled(String[] args) {
		boolean explicitlySet = System.getProperty(H2_CONSOLE_PROPERTY) != null
				|| System.getenv("SPRING_H2_CONSOLE_ENABLED") != null;
		if (args != null) {
			for (String arg : args) {
				if (arg != null && arg.startsWith("--" + H2_CONSOLE_PROPERTY)) {
					explicitlySet = true;
				}
			}
		}
		if (!explicitlySet) {
			// System properties take precedence over application.properties.
			System.setProperty(H2_CONSOLE_PROPERTY, "false");
			System.err.println("WARNING: H2 database console is disabled. To use it locally only, "
					+ "set SPRING_H2_CONSOLE_ENABLED=true; never enable it on a reachable server.");
		}
	}

}
