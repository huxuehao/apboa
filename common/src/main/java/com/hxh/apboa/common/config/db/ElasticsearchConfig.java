package com.hxh.apboa.common.config.db;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Elasticsearch vector database client configuration.
 *
 * @author huxuehao
 */
@Configuration
@ConditionalOnProperty(name = "rag.store", havingValue = "elasticsearch")
public class ElasticsearchConfig {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchConfig.class);

    @Bean(destroyMethod = "close")
    public RestClient elasticsearchRestClient(
            @Value("${rag.elasticsearch.uris:http://127.0.0.1:9200}") String uris,
            @Value("${rag.elasticsearch.username:}") String username,
            @Value("${rag.elasticsearch.password:}") String password,
            @Value("${rag.elasticsearch.api-key:}") String apiKey,
            @Value("${rag.elasticsearch.connect-timeout:5000}") int connectTimeout,
            @Value("${rag.elasticsearch.socket-timeout:60000}") int socketTimeout) {

        HttpHost[] hosts = parseHosts(uris);
        RestClientBuilder builder = RestClient.builder(hosts)
                .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                        .setConnectTimeout(connectTimeout)
                        .setSocketTimeout(socketTimeout));

        Header authHeader = buildAuthHeader(username, password, apiKey);
        if (authHeader != null) {
            builder.setDefaultHeaders(new Header[]{authHeader});
        }

        RestClient client = builder.build();
        log.info("Elasticsearch client initialized, uris={}", uris);
        return client;
    }

    private HttpHost[] parseHosts(String uris) {
        List<HttpHost> hosts = new ArrayList<>();
        for (String item : uris.split(",")) {
            String uriText = item.trim();
            if (uriText.isEmpty()) {
                continue;
            }
            if (!uriText.contains("://")) {
                uriText = "http://" + uriText;
            }
            URI uri = URI.create(uriText);
            String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
            int port = uri.getPort();
            if (port < 0) {
                port = "https".equalsIgnoreCase(scheme) ? 443 : 9200;
            }
            hosts.add(new HttpHost(uri.getHost(), port, scheme));
        }
        if (hosts.isEmpty()) {
            hosts.add(HttpHost.create("http://127.0.0.1:9200"));
        }
        return hosts.toArray(HttpHost[]::new);
    }

    private Header buildAuthHeader(String username, String password, String apiKey) {
        if (apiKey != null && !apiKey.isBlank()) {
            return new BasicHeader("Authorization", "ApiKey " + apiKey);
        }
        if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
            String token = username + ":" + password;
            String encoded = Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
            return new BasicHeader("Authorization", "Basic " + encoded);
        }
        return null;
    }
}
