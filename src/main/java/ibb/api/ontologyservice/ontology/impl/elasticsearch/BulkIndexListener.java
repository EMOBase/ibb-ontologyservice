package ibb.api.ontologyservice.ontology.impl.elasticsearch;

import java.util.List;

import co.elastic.clients.elasticsearch._helpers.bulk.BulkListener;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import io.quarkus.logging.Log;

class BulkIndexListener implements BulkListener<String> {
    @Override
    public void beforeBulk(long executionId, BulkRequest request, List<String> contexts) {}

    @Override
    public void afterBulk(long executionId, BulkRequest request, List<String> contexts, BulkResponse response) {
        if (response.errors()) {
            throw new PartialBulkException();
        }
    }

    @Override
    public void afterBulk(long executionId, BulkRequest request, List<String> contexts, Throwable failure) {
        Log.errorf("Bulk request %d failed", executionId, failure);
    }
}
