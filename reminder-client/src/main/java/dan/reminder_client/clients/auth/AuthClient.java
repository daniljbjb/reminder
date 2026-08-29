package dan.reminder_client.clients.auth;

import dan.reminder_client.clients.auth.config.AuthClientProperties;
import dan.reminder_client.dto.TokenResponse;
import dan.reminder_client.exceptions.AuthServiceConfigurationException;
import dan.reminder_client.exceptions.AuthServiceRejectedException;
import dan.reminder_client.exceptions.AuthServiceTemporaryException;
import dan.reminder_client.dto.ExchangeRequest;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Slf4j
public class AuthClient {

    private final RestClient restClient;
    private final AuthClientProperties props;

    public AuthClient(@Qualifier("authRestClient") RestClient restClient,
            AuthClientProperties props) {
        this.restClient = restClient;
        this.props = props;
    }

    public String exchangeCodeForJwt(String code) {

        if (code == null || code.isBlank()) {
            throw new AuthServiceRejectedException("code is blank");
        }

        try {
            TokenResponse response = restClient.post()
                    .uri(props.exchangePath())
                    .body(new ExchangeRequest(code))
                    .retrieve()
                    .onStatus(status -> status.value() == 400, (req, res) -> {
                        log.warn("Auth exchange code was rejected: status={}",
                                res.getStatusCode());
                        throw new AuthServiceRejectedException(
                                "Auth-service rejected one-time code: " + res.getStatusCode());
                    })
                    .onStatus(status -> status.value() == 401 || status.value() == 403,
                            (req, res) -> {
                                log.error("Auth exchange service credentials were rejected: status={}",
                                        res.getStatusCode());
                                throw new AuthServiceConfigurationException(
                                        "Auth-service rejected reminder-client credentials: "
                                                + res.getStatusCode());
                            })
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        log.error("Unexpected auth exchange client error: status={}",
                                res.getStatusCode());
                        throw new AuthServiceConfigurationException(
                                "Unexpected auth-service exchange response: "
                                        + res.getStatusCode());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        log.warn("Auth exchange server error: status={}",
                                res.getStatusCode());
                        throw new AuthServiceTemporaryException(
                                "Auth-service server error: " + res.getStatusCode());
                    })
                    .body(TokenResponse.class);

            String token = (response == null) ? null : response.accessToken();

            if (token == null || token.isBlank()) {
                throw new AuthServiceTemporaryException(
                        "Empty token response from auth-service");
            }

            log.debug("JWT successfully received from auth-service");
            return token;

        } catch (AuthServiceRejectedException
                | AuthServiceConfigurationException
                | AuthServiceTemporaryException e) {
            throw e;
        } catch (RestClientException e) {
            log.warn("Auth exchange transport failure: reason={}", e.toString());
            throw new AuthServiceTemporaryException(
                    "Auth-service transport error", e);
        }
    }
}
