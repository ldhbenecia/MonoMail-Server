package woozlabs.echo.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import woozlabs.echo.global.interceptor.FirebaseAuthInterceptor;

@Configuration
@Profile("prod")
@RequiredArgsConstructor
public class ProdCorsConfig implements WebMvcConfigurer {

    private final FirebaseAuthInterceptor firebaseAuthInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "https://app-dev.monomail.co",
                        "https://app.monomail.co",
                        "https://dev.monomail.co",
                        "https://monomail.co"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD")
                .allowedHeaders("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With",
                        "Cache-Control", "Pragma", "Cookie", "DNT", "User-Agent", "X-Custom-Header")
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
                        "/api/v1/echo/**",
                        "/api/v1/gen/**"
                );
    }
}
