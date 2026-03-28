package com.laetienda.webapp_test;

import com.laetienda.webapp_test.test.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.client.HttpStatusCodeException;

@SpringBootApplication
public class WebappTestApplication implements CommandLineRunner, ExitCodeGenerator {
	final private static Logger log = LoggerFactory.getLogger(WebappTestApplication.class);

	private int exitCode;
	private String message;

	@Autowired User testUser;

	public static void main(String[] args) {
		System.exit(
			SpringApplication.exit(
				SpringApplication.run(WebappTestApplication.class, args)
			)
		);
	}

	@Override
	public void run(String[] args){
		log.info("Testing application....");

		try{
			testUser.run();
			this.exitCode = 0;
			this.message = "Congratulations!. All tests completed successfully.";
		}catch(HttpStatusCodeException e){
			this.exitCode = e.getStatusCode().value();
			this.message = e.getMessage();
		}catch(AssertionError e){
			this.exitCode = 1;
			this.message = e.getMessage();
		}
	}

	@Override
	public int getExitCode(){
		log.info("...Test application finished.");
		log.info(message);
		log.info("Exit code: {}", this.exitCode);
		return this.exitCode;
	}
}
