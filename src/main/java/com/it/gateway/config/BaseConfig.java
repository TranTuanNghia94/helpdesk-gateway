// package com.it.gateway.config;

// import org.springframework.context.annotation.Configuration;
// import org.springframework.context.annotation.EnableAspectJAutoProxy;
// import org.springframework.scheduling.annotation.EnableScheduling;
// import org.springframework.web.servlet.config.annotation.CorsRegistry;
// import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
// import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// @Configuration
// @EnableScheduling
// @EnableAspectJAutoProxy
// @Slf4j
// public class BaseConfig implements WebMvcConfigurer {
//     private final LoggingInterceptor loggingInterceptor;

//     public BaseConfig(LoggingInterceptor loggingInterceptor) {
//         this.loggingInterceptor = loggingInterceptor;
//     }

//     @Override
//     public void addCorsMappings(CorsRegistry registry) {
//         registry.addMapping("/**")
//                 .allowedOrigins("*")
//                 .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
//                 .allowedHeaders("*")
//                 .maxAge(3600);
//     }

//     @Override
//     public void addInterceptors(InterceptorRegistry registry) {
//         registry.addInterceptor(loggingInterceptor);
//     }

// }
