package com.hxh.apboa.core.rag.store.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxh.apboa.core.rag.EmbeddingRecord;
import com.hxh.apboa.core.rag.RetrievalResult;
import com.hxh.apboa.core.rag.store.VectorStore;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch vector storage service.
 *
 * @author huxuehao
 */
@Component
@ConditionalOnProperty(name = "rag.store", havingValue = "elasticsearch")
public class ElasticsearchVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchVectorStore.class);

    private static final int[] SUPPORTED_DIMENSIONS = {64, 128, 256, 512, 768, 1024, 2048, 2560};
    private static final ContentType NDJSON = ContentType.create("application/x-ndjson", "UTF-8");
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String indexPrefix;

    public ElasticsearchVectorStore(@Autowired(required = false) RestClient restClient,
                                    ObjectMapper objectMapper,
                                    @Value("${rag.elasticsearch.index-prefix:apboa_rag}") String indexPrefix) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.indexPrefix = indexPrefix;
        if (restClient != null) {
            initIndexes();
        } else {
            log.warn("Elasticsearch client is not configured, local RAG is unavailable");
        }
    }

    private void initIndexes() {
        for (int dimension : SUPPORTED_DIMENSIONS) {
            try {
                ensureIndexExists(dimension);
            } catch (Exception e) {
                log.error("Failed to initialize Elasticsearch index, dimension={}", dimension, e);
            }
        }
        log.info("Elasticsearch vector indexes initialized, count={}", SUPPORTED_DIMENSIONS.length);
    }

    private String getIndexName(int dimension) {
        return indexPrefix + "_" + dimension;
    }

    private void ensureIndexExists(int dimension) throws IOException {
        String indexName = getIndexName(dimension);
        if (indexExists(indexName)) {
            return;
        }

        Map<String, Object> body = Map.of(
                "settings", Map.of(
                        "index", Map.of(
                                "number_of_shards", 1,
                                "number_of_replicas", 0
                        )
                ),
                "mappings", Map.of(
                        "properties", Map.of(
                                "chunk_id", Map.of("type", "long"),
                                "document_id", Map.of("type", "long"),
                                "knowledge_base_config_id", Map.of("type", "long"),
                                "embedding", Map.of(
                                        "type", "dense_vector",
                                        "dims", dimension,
                                        "index", true,
                                        "similarity", "cosine"
                                )
                        )
                )
        );

        performRequest("PUT", "/" + indexName, body);
        log.info("Created Elasticsearch vector index: {}, dimension={}", indexName, dimension);
    }

    private boolean indexExists(String indexName) throws IOException {
        Request request = new Request("HEAD", "/" + indexName);
        try {
            Response response = restClient.performRequest(request);
            return response.getStatusLine().getStatusCode() == 200;
        } catch (ResponseException e) {
            if (e.getResponse().getStatusLine().getStatusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public boolean isAvailable() {
        return restClient != null;
    }

    @Override
    public void storeEmbedding(Long id, Long chunkId, Long documentId,
                               Long knowledgeBaseConfigId, float[] embedding) {
        if (!isAvailable()) {
            throw new RuntimeException("Elasticsearch client is not configured");
        }

        try {
            ensureIndexExists(embedding.length);
            Map<String, Object> doc = buildDocument(chunkId, documentId, knowledgeBaseConfigId, embedding);
            performRequest("PUT", "/" + getIndexName(embedding.length) + "/_doc/" + id + "?refresh=true", doc);
        } catch (Exception e) {
            throw new RuntimeException("Elasticsearch vector store failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void storeEmbeddings(List<EmbeddingRecord> records) {
        if (!isAvailable()) {
            throw new RuntimeException("Elasticsearch client is not configured");
        }
        if (records.isEmpty()) {
            return;
        }

        StringBuilder bulkBody = new StringBuilder();
        try {
            for (EmbeddingRecord record : records) {
                ensureIndexExists(record.embedding().length);
                Map<String, Object> action = Map.of(
                        "index", Map.of(
                                "_index", getIndexName(record.embedding().length),
                                "_id", record.id().toString()
                        )
                );
                bulkBody.append(toJson(action)).append('\n');
                bulkBody.append(toJson(buildDocument(
                        record.chunkId(),
                        record.documentId(),
                        record.knowledgeBaseConfigId(),
                        record.embedding()
                ))).append('\n');
            }

            Response response = performRequest("POST", "/_bulk?refresh=true", bulkBody.toString(), NDJSON);
            Map<String, Object> responseBody = readResponse(response);
            if (Boolean.TRUE.equals(responseBody.get("errors"))) {
                throw new RuntimeException("Elasticsearch bulk response contains failed items");
            }
        } catch (Exception e) {
            throw new RuntimeException("Elasticsearch vector batch store failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<RetrievalResult> search(float[] queryEmbedding, Long knowledgeBaseConfigId,
                                        int limit, double scoreThreshold) {
        if (!isAvailable()) {
            throw new RuntimeException("Elasticsearch client is not configured");
        }

        try {
            String indexName = getIndexName(queryEmbedding.length);
            if (!indexExists(indexName)) {
                return List.of();
            }

            Map<String, Object> body = Map.of(
                    "size", limit,
                    "_source", List.of("chunk_id", "document_id"),
                    "query", Map.of(
                            "script_score", Map.of(
                                    "query", Map.of(
                                            "term", Map.of(
                                                    "knowledge_base_config_id", knowledgeBaseConfigId
                                            )
                                    ),
                                    "script", Map.of(
                                            "source", "cosineSimilarity(params.query_vector, 'embedding') + 1.0",
                                            "params", Map.of(
                                                    "query_vector", floatList(queryEmbedding)
                                            )
                                    )
                            )
                    )
            );

            Response response = performRequest("POST", "/" + indexName + "/_search", body);
            Map<String, Object> responseBody = readResponse(response);
            return parseResults(responseBody, scoreThreshold);
        } catch (Exception e) {
            throw new RuntimeException("Elasticsearch vector search failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteByDocumentId(Long documentId) {
        deleteByField("document_id", documentId);
    }

    @Override
    public void deleteByKnowledgeBaseConfigId(Long knowledgeBaseConfigId) {
        deleteByField("knowledge_base_config_id", knowledgeBaseConfigId);
    }

    @Override
    public void deleteByChunkId(Long chunkId) {
        deleteByField("chunk_id", chunkId);
    }

    private void deleteByField(String fieldName, Long value) {
        if (!isAvailable()) {
            return;
        }
        Map<String, Object> body = Map.of(
                "query", Map.of(
                        "term", Map.of(fieldName, value)
                )
        );
        try {
            performRequest("POST", "/" + indexPrefix + "_*/_delete_by_query?conflicts=proceed&refresh=true", body);
        } catch (ResponseException e) {
            if (e.getResponse().getStatusLine().getStatusCode() != 404) {
                log.warn("Elasticsearch delete failed, field={}, value={}: {}", fieldName, value, e.getMessage());
            }
        } catch (Exception e) {
            log.warn("Elasticsearch delete failed, field={}, value={}: {}", fieldName, value, e.getMessage());
        }
    }

    private Map<String, Object> buildDocument(Long chunkId, Long documentId,
                                              Long knowledgeBaseConfigId, float[] embedding) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("chunk_id", chunkId);
        doc.put("document_id", documentId);
        doc.put("knowledge_base_config_id", knowledgeBaseConfigId);
        doc.put("embedding", floatList(embedding));
        return doc;
    }

    private List<Float> floatList(float[] embedding) {
        List<Float> vector = new ArrayList<>(embedding.length);
        for (float v : embedding) {
            vector.add(v);
        }
        return vector;
    }

    private Response performRequest(String method, String endpoint, Map<String, Object> body) throws IOException {
        return performRequest(method, endpoint, toJson(body));
    }

    private Response performRequest(String method, String endpoint, String body) throws IOException {
        return performRequest(method, endpoint, body, ContentType.APPLICATION_JSON);
    }

    private Response performRequest(String method, String endpoint, String body, ContentType contentType) throws IOException {
        Request request = new Request(method, endpoint);
        request.setEntity(new NStringEntity(body, contentType));
        return restClient.performRequest(request);
    }

    private String toJson(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }

    private Map<String, Object> readResponse(Response response) throws IOException {
        return objectMapper.readValue(response.getEntity().getContent(), MAP_TYPE);
    }

    @SuppressWarnings("unchecked")
    private List<RetrievalResult> parseResults(Map<String, Object> responseBody, double scoreThreshold) {
        Object hitsObject = responseBody.get("hits");
        if (!(hitsObject instanceof Map<?, ?> hitsMap)) {
            return List.of();
        }

        Object hitListObject = hitsMap.get("hits");
        if (!(hitListObject instanceof List<?> hitList)) {
            return List.of();
        }

        List<RetrievalResult> results = new ArrayList<>();
        for (Object item : hitList) {
            if (!(item instanceof Map<?, ?> hit)) {
                continue;
            }
            Object sourceObject = hit.get("_source");
            Object scoreObject = hit.get("_score");
            if (!(sourceObject instanceof Map<?, ?> source) || !(scoreObject instanceof Number scoreNumber)) {
                continue;
            }

            Object chunkIdObject = source.get("chunk_id");
            Object documentIdObject = source.get("document_id");
            if (!(chunkIdObject instanceof Number chunkId) || !(documentIdObject instanceof Number documentId)) {
                continue;
            }

            double score = scoreNumber.doubleValue() - 1.0;
            if (score >= scoreThreshold) {
                results.add(new RetrievalResult(chunkId.longValue(), documentId.longValue(), score));
            }
        }
        return results;
    }
}
