package com.netpulse.node.repository;

import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.entity.NodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NodeRepository extends JpaRepository<NetworkNode, Long> {
    Optional<NetworkNode> findByNodeId(String nodeId);
    boolean existsByNodeId(String nodeId);
    List<NetworkNode> findByStatus(NodeStatus status);
    long countByStatus(NodeStatus status);
}
