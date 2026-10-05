package com.netpulse.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * WebMvc configuration to serve modern SPA frontend static assets and forward client-side routes.
 */
@Configuration
public class SpaWebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/", "classpath:/public/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }
                        // Don't forward API, WebSocket, Swagger, or Actuator paths to index.html
                        if (resourcePath.startsWith("api/") ||
                            resourcePath.startsWith("ws/") ||
                            resourcePath.startsWith("actuator/") ||
                            resourcePath.startsWith("swagger-ui") ||
                            resourcePath.startsWith("api-docs")) {
                            return null;
                        }
                        // Forward all client-side navigation routes to index.html
                        return location.createRelative("index.html");
                    }
                });
    }
}
