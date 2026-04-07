package com.sweetbook.server.sweetbook.config;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

@Configuration
@Slf4j
public class SweetbookClientConfig {

    @Bean
    public RestClient sweetbookRestClient(SweetbookProperties properties) {
        disableSslVerificationGlobally();
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestInterceptor((request, body, execution) -> {
                    // Always enforce Sweetbook API key, never forward app JWT to Sweetbook.
                    request.getHeaders().setBearerAuth(properties.apiKey());
                    return execution.execute(request, body);
                })
                .requestFactory(sweetbookRequestFactory(properties))
                .build();
    }

    private ClientHttpRequestFactory sweetbookRequestFactory(SweetbookProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) properties.connectTimeout().toMillis());
        requestFactory.setReadTimeout((int) properties.readTimeout().toMillis());
        return requestFactory;
    }

    private void disableSslVerificationGlobally() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }

                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception exception) {
            log.warn("Failed to disable SSL verification for Sweetbook RestClient. reason={}", exception.getMessage(), exception);
        }
    }
}
