package org.wl.wllock.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Created by wanglin6 on 2017/3/28.
 */
@Configuration
@ComponentScan(basePackages = "org.wl.wllock")
@EnableConfigurationProperties(LockSettings.class)
public class LockAutoConfiguration {


}
