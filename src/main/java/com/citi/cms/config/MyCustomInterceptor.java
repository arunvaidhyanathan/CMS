package com.citi.cms.config;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class MyCustomInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(MyCustomInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        long startTime = System.currentTimeMillis();
        request.setAttribute("startTime", startTime);

        logger.info("--- preHandle called ---");
        logger.info("Request URI: {}", request.getRequestURI());
        logger.info("Method: {}", request.getMethod());
        logger.info("Handler: {}", handler);

        // ... (authentication/authorization logic if any)
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {

        logger.info("--- postHandle called ---");
        logger.info("Request URI: {}", request.getRequestURI());
        logger.info("Handler: {}", handler);

        // Check if modelAndView is null (e.g., for @ResponseBody methods, direct HttpServletResponse writes,
        // or cases where no view resolution is expected).
        if (modelAndView == null) {
            logger.debug("ModelAndView is null (likely a @ResponseBody method, direct HttpServletResponse manipulation, or AsyncRequest).");
        } else {
            String viewName = modelAndView.getViewName();

            if (viewName != null) {
                if (viewName.startsWith("redirect:")) {
                    logger.debug("ModelAndView is a redirect view to: {}", viewName);
                } else if (viewName.startsWith("forward:")) {
                    logger.debug("ModelAndView is a forward view to: {}", viewName);
                } else {
                    // It's a regular view name, not a redirect or forward
                    logger.info("View Name: {}", viewName);
                    logger.info("Model: {}", modelAndView.getModel());

                    // Example: Add common data to the model for all views that are NOT redirects/forwards
                    modelAndView.addObject("currentYear", java.time.Year.now().getValue());
                    modelAndView.addObject("appName", "My Spring MVC App");
                    logger.debug("Added common model attributes.");
                }
            } else {
                // modelAndView exists but viewName is null.
                // This can happen if a View object was directly set (e.g., modelAndView.setView(someViewObject);)
                // instead of a view name, or if it's implicitly handled.
                logger.debug("ModelAndView has no view name set (possibly direct View object or implicit handling).");
            }
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) throws Exception {
        logger.info("--- afterCompletion called ---");
        logger.info("Request URI: {}", request.getRequestURI());
        logger.info("Handler: {}", handler);

        long startTime = (Long) request.getAttribute("startTime");
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        logger.info("Request to {} took {} ms.", request.getRequestURI(), executionTime);

        if (ex != null) {
            logger.error("Request to {} completed with an exception: {}", request.getRequestURI(), ex.getMessage(), ex);
        } else {
            logger.debug("Request to {} completed successfully.", request.getRequestURI());
        }
    }
}