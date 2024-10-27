package woozlabs.echo.global.config;

import java.time.Duration;
import java.util.Arrays;
import org.apache.hc.client5.http.HttpRequestRetryStrategy;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import woozlabs.echo.domain.chatGPT.ChatGPTInterface;
import woozlabs.echo.domain.gemini.GeminiInterface;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        // RestTemplateBuilder로 RestTemplate 생성
        RestTemplate restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10)) // 연결 타임아웃
                .setReadTimeout(Duration.ofSeconds(10))    // 읽기 타임아웃
                .build();

        // HttpComponentsClientHttpRequestFactory 설정
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(createHttpClient());
        restTemplate.setRequestFactory(factory);

        // 메시지 변환기 설정
        restTemplate.setMessageConverters(Arrays.asList(
                new MappingJackson2HttpMessageConverter(),
                new FormHttpMessageConverter()
        ));

        return restTemplate;
    }

    private CloseableHttpClient createHttpClient() {
        // 요청 설정
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(10))
                .setResponseTimeout(Timeout.ofSeconds(10))
                .build();

        // 연결 풀링 및 소켓 설정
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(10))
                .setSocketTimeout(Timeout.ofSeconds(10))
                .setValidateAfterInactivity(Timeout.ofSeconds(10))
                .build();

        SocketConfig socketConfig = SocketConfig.custom()
                .setSoTimeout(Timeout.ofSeconds(10))
                .setSoKeepAlive(true) // Keep-alive 활성화
                .setTcpNoDelay(true)  // Nagle's algorithm 비활성화
                .build();

        // HttpRequestRetryStrategy를 사용하여 재시도 전략 설정
        HttpRequestRetryStrategy retryStrategy = new DefaultHttpRequestRetryStrategy(3,
                TimeValue.ofSeconds(1)); // 최대 3회 재시도, 1초 대기

        return HttpClientBuilder.create()
                .setConnectionManager(createConnectionManager(connectionConfig, socketConfig))
                .setDefaultRequestConfig(requestConfig)
                .setRetryStrategy(retryStrategy) // HttpRequestRetryStrategy 설정
                .build();
    }

    private HttpClientConnectionManager createConnectionManager(ConnectionConfig connectionConfig,
                                                                SocketConfig socketConfig) {
        return PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(200) // 전체 최대 연결 수
                .setMaxConnPerRoute(50) // 각 라우트당 최대 연결 수
                .setDefaultConnectionConfig(connectionConfig) // 기본 연결 설정
                .setDefaultSocketConfig(socketConfig) // 기본 소켓 설정
                .build();
    }

    @Bean
    public RestClient geminiRestClient(@Value("${gemini.api.url}") String apiUrl,
                                       @Value("${gemini.api.key}") String apiKey) {
        return RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("x-goog-api-key", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Bean
    public GeminiInterface geminiInterface(@Qualifier("geminiRestClient") RestClient client) {
        RestClientAdapter adapter = RestClientAdapter.create(client);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(GeminiInterface.class);
    }

    @Bean
    public RestClient chatGPTRestClient(@Value("${chatgpt.api.url}") String apiUrl,
                                        @Value("${chatgpt.api.key}") String apiKey) {
        return RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Bean
    public ChatGPTInterface chatGPTInterface(@Qualifier("chatGPTRestClient") RestClient client) {
        RestClientAdapter adapter = RestClientAdapter.create(client);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(ChatGPTInterface.class);
    }
}
