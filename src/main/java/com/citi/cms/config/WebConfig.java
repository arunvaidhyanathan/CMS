package com.citi.cms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final MyCustomInterceptor myCustomInterceptor;

    // Inject your custom interceptor as a Spring bean
    // This requires MyCustomInterceptor to be a @Component or defined as a @Bean in another config.
    public WebConfig(MyCustomInterceptor myCustomInterceptor) {
        this.myCustomInterceptor = myCustomInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Register your custom interceptor
        registry.addInterceptor(myCustomInterceptor)
                .addPathPatterns("/**") // Apply to all incoming requests
                .excludePathPatterns("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico"); // Exclude static resources

        // You can add more interceptors with different path patterns if needed
        // registry.addInterceptor(new AnotherInterceptor()).addPathPatterns("/admin/**");
    }
}