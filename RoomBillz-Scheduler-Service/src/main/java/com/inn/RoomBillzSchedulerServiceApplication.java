package com.inn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;

@SpringBootApplication
@EnableFeignClients
@EnableDiscoveryClient
@EnableAspectJAutoProxy
@OpenAPIDefinition(
	    info = @Info(
	        title = "RoomBillz Scheduler Service",
	        version = "v1",
	        description = "Microservice that manages and runs scheduled background jobs for the RoomBillz system.",
	        contact = @Contact(
	            name = "Sarfaraz Alam",
	            email = "sarfarazalam2702@gmail.com",
	            url = "https://www.linkedin.com/in/alam-sarfaraz/"
	        ),
	        license = @License(
	            name = "Apache 2.0",
	            url = "http://www.apache.org/licenses/LICENSE-2.0.html"
	        )
	    ),
	    servers = {
	        @Server(
	            url = "http://localhost:8083/api/v1/scheduler",
	            description = "Local Scheduler Service"
	        ),
	        @Server(
		            url = "https://qa/api.roombillz.com/api/v1/scheduler",
		            description = "QA Server"
		        ),
	        @Server(
	            url = "https://api.roombillz.com/api/v1/scheduler",
	            description = "Production Server"
	        )
	    },
	    externalDocs = @ExternalDocumentation(
	        description = "RoomBillz Scheduler API Documentation",
	        url = "https://roombillz.com/docs/scheduler"
	    )
	)
public class RoomBillzSchedulerServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(RoomBillzSchedulerServiceApplication.class, args);
		System.out.println("************************************************** RoomBillz Scheduler Server Started Successfully ************************************************************");

	}

}
