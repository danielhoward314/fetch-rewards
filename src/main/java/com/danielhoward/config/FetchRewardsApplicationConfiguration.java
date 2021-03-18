package com.danielhoward.config;

import com.danielhoward.fetchrewards.FetchRewardsApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableConfigurationProperties
@Import({
    CacheConfig.class
})
@ComponentScan(basePackageClasses = {FetchRewardsApplication.class })
public class FetchRewardsApplicationConfiguration {
}
