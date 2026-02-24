package jy.Job_Flow_Agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class JobFlowAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobFlowAgentApplication.class, args);

		log.info("안녕하세요! JobFlowAgent Activated!");

		

	}

}
