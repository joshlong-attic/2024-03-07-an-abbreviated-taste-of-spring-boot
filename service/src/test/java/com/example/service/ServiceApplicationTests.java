package com.example.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

@SpringBootTest
class ServiceApplicationTests {

	@Test
	void contextLoads() {

		var am = ApplicationModules.of(ServiceApplication.class);
		am.verify() ;

		System.out.println(am);

		new Documenter(am).writeDocumentation();
	}

}
