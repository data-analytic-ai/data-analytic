package ai.dataanalytic.querybridge.mongodb.repository;

import ai.dataanalytic.querybridge.dto.ConnectionEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ConnectionRepository extends MongoRepository<ConnectionEntity, String> {
    List<ConnectionEntity> findByUserId(String userId);
    ConnectionEntity findByUserIdAndConnectionId(String userId, String connectionId);
}
