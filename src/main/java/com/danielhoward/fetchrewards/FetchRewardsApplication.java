package com.danielhoward.fetchrewards;

import com.danielhoward.config.FetchRewardsApplicationConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Import;

@Import({
	FetchRewardsApplicationConfiguration.class
})
@SpringBootApplication
public class FetchRewardsApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder(FetchRewardsApplication.class).build().run(args);
	}

}
