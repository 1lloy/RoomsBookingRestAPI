package com.illoy.roombooking;

import org.springframework.boot.SpringApplication;

public class TestRoombookingApplication {

	public static void main(String[] args) {
		SpringApplication.from(RoombookingApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
