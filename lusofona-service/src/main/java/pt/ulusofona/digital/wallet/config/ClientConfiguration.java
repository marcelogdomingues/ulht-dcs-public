package pt.ulusofona.digital.wallet.config;

import feign.Logger;
import feign.Request;
import feign.Response;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import feign.okhttp.OkHttpClient;
import okhttp3.java.net.cookiejar.JavaNetCookieJar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;

import static java.util.concurrent.TimeUnit.SECONDS;

@Configuration
public class ClientConfiguration {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new CustomErrorDecoder();
    }

    @Bean
    public OkHttpClient client() {
        // Create a cookie manager that accepts all cookies
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        
        // Create OkHttpClient with cookie handling and timeouts
        // Connect timeout: 5 seconds, Read timeout: 10 seconds, Write timeout: 10 seconds
        okhttp3.OkHttpClient httpClient = new okhttp3.OkHttpClient.Builder()
                .cookieJar(new JavaNetCookieJar(cookieManager))
                .connectTimeout(5, SECONDS)
                .readTimeout(10, SECONDS)
                .writeTimeout(10, SECONDS)
                .build();
        
        return new OkHttpClient(httpClient);
    }

    @Bean
    public Retryer retryer() {
        return new Retryer.Default(100, SECONDS.toMillis(1), 5);
    }

    @Bean
    public Logger feignLogger() {
        return new Logger() {
            private final org.slf4j.Logger logger = LoggerFactory.getLogger("FEIGN_RAW_RESPONSE");

            @Override
            protected void log(String configKey, String format, Object... args) {
                // Always log to ensure we see the output
                logger.info(String.format(methodTag(configKey) + format, args));
            }

            @Override
            protected void logRequest(String configKey, Level logLevel, Request request) {
                logger.info("=== FEIGN REQUEST ===");
                logger.info("Method: {}", request.httpMethod());
                logger.info("URL: {}", request.url());
                logger.info("Headers: {}", request.headers());
                if (request.body() != null) {
                    logger.info("Body: {}", new String(request.body(), java.nio.charset.StandardCharsets.UTF_8));
                }
            }

            @Override
            protected Response logAndRebufferResponse(String configKey, Level logLevel, Response response, long elapsedTime) throws IOException {
                logger.info("=== FEIGN RESPONSE ===");
                logger.info("Status: {}", response.status());
                logger.info("Headers: {}", response.headers());
                logger.info("Elapsed Time: {} ms", elapsedTime);
                
                // Read and log the response body
                if (response.body() != null) {
                    byte[] bodyData = response.body().asInputStream().readAllBytes();
                    String responseBody = new String(bodyData, java.nio.charset.StandardCharsets.UTF_8);
                    logger.info("Raw Response Body: {}", responseBody);
                    
                    // Return a new response with the same data since we consumed the stream
                    return response.toBuilder()
                            .body(bodyData)
                            .build();
                }
                return response;
            }
        };
    }
}