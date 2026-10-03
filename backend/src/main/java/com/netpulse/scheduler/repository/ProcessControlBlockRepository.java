package com.netpulse.scheduler.repository;

import com.netpulse.scheduler.entity.ProcessControlBlock;
import com.netpulse.scheduler.state.ProcessState;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessControlBlockRepository extends JpaRepository<ProcessControlBlock, Long> {

    Optional<ProcessControlBlock> findByProcessId(String processId);

    List<ProcessControlBlock> findByState(ProcessState state);

    List<ProcessControlBlock> findByTargetNodeId(String targetNodeId);

    List<ProcessControlBlock> findByTargetNodeIdAndState(String targetNodeId, ProcessState state);

    @Query("SELECT p FROM ProcessControlBlock p WHERE p.state IN (:states) ORDER BY p.priority ASC, p.arrivalTime ASC")
    List<ProcessControlBlock> findActiveProcesses(@Param("states") List<ProcessState> states);

    @Query("SELECT p FROM ProcessControlBlock p ORDER BY p.createdAt DESC")
    List<ProcessControlBlock> findRecentProcesses(Pageable pageable);

    @Query("SELECT COUNT(p) FROM ProcessControlBlock p WHERE p.state = :state")
    long countByState(@Param("state") ProcessState state);

    void deleteByProcessId(String processId);
}
