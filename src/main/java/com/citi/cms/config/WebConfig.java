package com.citi.cms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebConfig.class);

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        logger.info("Configuring CORS mappings");
        
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods(
                    HttpMethod.GET.name(),
                    HttpMethod.POST.name(),
                    HttpMethod.PUT.name(),
                    HttpMethod.DELETE.name(),
                    HttpMethod.OPTIONS.name()
                )
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        registry.addMapping("/auth/**")
                .allowedOriginPatterns("*")
                .allowedMethods(HttpMethod.POST.name(), HttpMethod.OPTIONS.name())
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        logger.info("CORS configuration completed");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        logger.info("Configuring static resource handlers");
        
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600); // 1 hour cache

        registry.addResourceHandler("/public/**")
                .addResourceLocations("classpath:/public/")
                .setCachePeriod(3600);

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/")
                .setCachePeriod(3600);

        // Swagger UI resources (if needed)
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/springfox-swagger-ui/")
                .setCachePeriod(3600);

        logger.info("Static resource handlers configured");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        logger.info("Configuring request interceptors");
        
        registry.addInterceptor(new RequestLoggingInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/health", "/api/version");

        logger.info("Request interceptors configured");
    }

    @Bean
    public RequestLoggingInterceptor requestLoggingInterceptor() {
        return new RequestLoggingInterceptor();
    }

    /**
     * Custom interceptor for request logging and monitoring
     */
    public static class RequestLoggingInterceptor implements HandlerInterceptor {
        
        private static final Logger requestLogger = LoggerFactory.getLogger("REQUEST_LOGGER");

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            long startTime = System.currentTimeMillis();
            request.setAttribute("startTime", startTime);

            // Log request details
            String method = request.getMethod();
            String uri = request.getRequestURI();
            String queryString = request.getQueryString();
            String clientIP = getClientIpAddress(request);
            String userAgent = request.getHeader(HttpHeaders.USER_AGENT);

            requestLogger.info("Request started - Method: {}, URI: {}, Query: {}, Client IP: {}, User-Agent: {}", 
                             method, uri, queryString, clientIP, userAgent);

            // Add security headers
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("X-Frame-Options", "DENY");
            response.setHeader("X-XSS-Protection", "1; mode=block");
            response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");

            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
            Long startTime = (Long) request.getAttribute("startTime");
            if (startTime != null) {
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;

                String method = request.getMethod();
                String uri = request.getRequestURI();
                int status = response.getStatus();

                if (ex != null) {
                    requestLogger.error("Request completed with error - Method: {}, URI: {}, Status: {}, Duration: {}ms, Error: {}", 
                                      method, uri, status, duration, ex.getMessage());
                } else {
                    requestLogger.info("Request completed - Method: {}, URI: {}, Status: {}, Duration: {}ms", 
                                     method, uri, status, duration);
                }
            }
        }

        /**
         * Get the real client IP address from various headers
         */
        private String getClientIpAddress(HttpServletRequest request) {
            String xForwardedForHeader = request.getHeader("X-Forwarded-For");
            if (xForwardedForHeader != null && !xForwardedForHeader.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedForHeader)) {
                return xForwardedForHeader.split(",")[0].trim();
            }

            String xRealIpHeader = request.getHeader("X-Real-IP");
            if (xRealIpHeader != null && !xRealIpHeader.isEmpty() && !"unknown".equalsIgnoreCase(xRealIpHeader)) {
                return xRealIpHeader;
            }

            String xForwardedHeader = request.getHeader("X-Forwarded");
            if (xForwardedHeader != null && !xForwardedHeader.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedHeader)) {
                return xForwardedHeader;
            }

            String xClusterClientIpHeader = request.getHeader("X-Cluster-Client-IP");
            if (xClusterClientIpHeader != null && !xClusterClientIpHeader.isEmpty() && !"unknown".equalsIgnoreCase(xClusterClientIpHeader)) {
                return xClusterClientIpHeader;
            }

            return request.getRemoteAddr();
        }
    }
}