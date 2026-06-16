package com.trekmate.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path buildDir = Paths.get(System.getProperty("user.dir"))
                .resolve("..")
                .resolve("TrekMateFE")
                .resolve("build")
                .normalize()
                .toAbsolutePath();

        registry.addResourceHandler("/**")
                .addResourceLocations(buildDir.toUri().toString());
    }
}
