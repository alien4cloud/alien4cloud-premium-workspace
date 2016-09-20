package org.alien4cloud.workspace;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan({ "org.alien.workspace.rest", "org.alien.workspace.service" })
public class PluginConfiguration {
}
