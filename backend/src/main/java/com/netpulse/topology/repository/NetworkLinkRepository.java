package com.netpulse.topology.repository;

import com.netpulse.topology.entity.NetworkLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NetworkLinkRepository extends JpaRepository<NetworkLink, Long> {

    Optional<NetworkLink> findByLinkId(String linkId);

    boolean existsByLinkId(String linkId);

    boolean existsBySourceNodeIdAndTargetNodeId(String sourceNodeId, String targetNodeId);

    List<NetworkLink> findBySourceNodeId(String sourceNodeId);

    List<NetworkLink> findByTargetNodeId(String targetNodeId);

    @Query("SELECT l FROM NetworkLink l WHERE l.sourceNodeId = :nodeId OR l.targetNodeId = :nodeId")
    List<NetworkLink> findByNodeInvolvement(@Param("nodeId") String nodeId);

    @Query("SELECT COUNT(l) FROM NetworkLink l WHERE l.sourceNodeId = :nodeId OR l.targetNodeId = :nodeId")
    long countByNodeInvolvement(@Param("nodeId") String nodeId);

    @Modifying
    @Query("DELETE FROM NetworkLink l WHERE l.sourceNodeId = :nodeId OR l.targetNodeId = :nodeId")
    void deleteByNodeInvolvement(@Param("nodeId") String nodeId);
}
