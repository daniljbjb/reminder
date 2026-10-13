/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder_client.clients.auth.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 *
 * @author danil
 */
@Configuration
@EnableConfigurationProperties(AuthClientProperties.class)
public class AuthClientConfig {

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @Qualifier("authRestClient")
    RestClient authRestClient(RestClient.Builder builder, AuthClientProperties props) {
        return builder
                .requestFactory(clientHttpRequestFactory(props))
                .baseUrl(props.baseUrl())
                .defaultHeaders(h -> h.setBasicAuth(props.clientId(), props.clientSecret()))
                .build();
    }

    private ClientHttpRequestFactory clientHttpRequestFactory(AuthClientProperties props) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();

        long connectMs = props.timeouts().connect().toMillis();
        long readMs = props.timeouts().read().toMillis();

        f.setConnectTimeout(Math.toIntExact(connectMs));
        f.setReadTimeout(Math.toIntExact(readMs));

        return f;
    }
}
