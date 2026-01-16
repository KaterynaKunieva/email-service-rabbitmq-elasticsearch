package org.kunievakateryna.repository;

import org.kunievakateryna.data.EmailHistory;
import org.kunievakateryna.data.EmailStatus;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import java.util.List;

/**
 * Repository interface for managing EmailHistory documents in Elasticsearch
 */
public interface EmailHistoryRepository extends ElasticsearchRepository<EmailHistory, String> {

    /**
     * Finds a list of email history records by their current status
     *
     * @param status the status to search for
     * @return a list of matching EmailHistory records
     */
    List<EmailHistory> findByStatus(EmailStatus status);
}