package org.kunievakateryna.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

/**
 * Configuration for Elasticsearch client connection
 */
@Configuration
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.uris}")
    private String esAddress;

    /**
     * Configures the connection parameters for the Elasticsearch client
     *
     * @return ClientConfiguration with the server address
     */
    @Override
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
                .connectedTo(esAddress)
                .build();
    }
}