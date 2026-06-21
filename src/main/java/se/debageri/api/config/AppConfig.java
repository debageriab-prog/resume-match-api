package se.debageri.api.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;

@Configuration
public class AppConfig {

	@Bean
	public OpenAIClient openAIClient() {
		// Reads OPENAI_API_KEY from environment
		return OpenAIOkHttpClient.fromEnv();
	}

	@Bean(destroyMethod = "close")
	public RestClient restClient(@Value("${app.elastic.url}") String elasticUrl) {
		return RestClient.builder(HttpHost.create(elasticUrl)).build();
	}

	@Bean
	public ElasticsearchTransport elasticsearchTransport(RestClient restClient, ObjectMapper mapper) {
		return new RestClientTransport(restClient, new co.elastic.clients.json.jackson.JacksonJsonpMapper(mapper));
	}

	@Bean
	public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
		return new ElasticsearchClient(transport);
	}
}
