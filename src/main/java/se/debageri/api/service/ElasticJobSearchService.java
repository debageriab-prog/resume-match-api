package se.debageri.api.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;

@Service
public class ElasticJobSearchService {

	private final ElasticsearchClient elasticsearchClient;
	private final String index;

	public ElasticJobSearchService(ElasticsearchClient elasticsearchClient,
			@Value("${app.elastic.index}") String index) {
		this.elasticsearchClient = elasticsearchClient;
		this.index = index;
	}

	/**
	 * Deletes job documents for the provided assignment IDs using the bulk API. The
	 * method is idempotent — deleting a non-existing document is a no-op.
	 */
	public void deleteByAssignmentIds(Collection<Long> assignmentIds) {
		if (assignmentIds == null || assignmentIds.isEmpty())
			return;

		try {
			BulkRequest.Builder br = new BulkRequest.Builder();
			List<String> ids = new ArrayList<>();
			for (Long id : assignmentIds) {
				String sid = String.valueOf(id);
				br.operations(op -> op.delete(d -> d.index(index).id(sid)));
				ids.add(sid);
			}

			BulkResponse resp = elasticsearchClient.bulk(br.build());
			if (resp.errors()) {
				throw new RuntimeException("Bulk delete reported errors: " + resp.items().toString());
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to delete job docs from Elasticsearch: " + e.getMessage(), e);
		}
	}
}
