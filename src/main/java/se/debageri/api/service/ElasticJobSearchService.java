package se.debageri.api.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;

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

	/**
	 * K-nearest neighbors (KNN) search for job documents based on the provided
	 * query vector.
	 *
	 * @param queryVector
	 *            the embedding vector to search with
	 * @param k
	 *            the number of nearest neighbors to return
	 * @param numCandidates
	 *            the number of candidate neighbors to consider
	 * @return a list of job documents with their scores
	 */
	public List<Map<String, Object>> knnJobs(float[] queryVector, int k, int numCandidates) {
		try {
			List<Float> qv = new ArrayList<>(queryVector.length);
			for (float v : queryVector) {
				qv.add(v);
			}

			SearchResponse<JsonData> resp = elasticsearchClient.search(
					s -> s.index(index).size(k)
							.knn(knn -> knn.field("job_embedding").queryVector(qv).k(k).numCandidates(numCandidates)),
					JsonData.class);

			List<Map<String, Object>> out = new ArrayList<>();
			for (Hit<JsonData> hit : resp.hits().hits()) {
				Map<String, Object> m = new HashMap<>();
				m.put("_score", hit.score());
				m.putAll(hit.source().to(Map.class));
				out.add(m);
			}
			return out;

		} catch (Exception e) {
			throw new RuntimeException("Elasticsearch knn search failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Indexes a job document into Elasticsearch.
	 *
	 * @param doc
	 *            the job document to index (must contain "assignment_id" field)
	 */
	public void indexJobDocument(Map<String, Object> doc) {
		try {
			String id = String.valueOf(doc.get("assignment_id"));
			elasticsearchClient.index(i -> i.index(index).id(id).document(doc));
		} catch (Exception e) {
			throw new RuntimeException("Failed to index job document into Elasticsearch: " + e.getMessage(), e);
		}
	}

	/**
	 * Fetches the indexed assignment document by assignment_id (stored as ES
	 * document _id).
	 *
	 * @param assignmentId
	 *            internal assignment.id
	 * @return source map (includes title, job_text, job_embedding, etc.)
	 */
	public Map<String, Object> getAssignmentDoc(long assignmentId) {
		try {
			GetResponse<JsonData> resp = elasticsearchClient.get(g -> g.index(index).id(String.valueOf(assignmentId)),
					JsonData.class);

			if (!resp.found() || resp.source() == null) {
				throw new RuntimeException("Assignment doc not found in ES for assignment_id=" + assignmentId);
			}
			return resp.source().to(Map.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to get assignment doc from ES assignment_id=" + assignmentId, e);
		}
	}
}
