package woozlabs.echo.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import woozlabs.echo.global.interceptor.FirebaseAuthInterceptor;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final FirebaseAuthInterceptor firebaseAuthInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "http://localhost:3000",
                        "http://localhost:5173",
                        "https://echo-email-app.firebaseapp.com",
                        "https://echo-email-app.web.app"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(firebaseAuthInterceptor)
                .addPathPatterns(
                        "/api/v1/gmail/**",
                        "/api/v1/calendar/**",
                        "/api/v1/gemini/**",
                        "/api/v1/fcm",
                        "/api/v1/echo/**"
                );
    }
}
