package com.citi.cms.repository;

import com.citi.cms.entity.WorkItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkItemRepository extends JpaRepository<WorkItem, Long> {
    
    Optional<WorkItem> findByWorkItemId(Long workItemId);
    Optional<WorkItem> findByTaskKey(Long taskKey);
    List<WorkItem> findByAssignedToUserIdAndStatus(Long userId, String status);
    List<WorkItem> findByStatus(String status);
    
    @Query("SELECT wi FROM WorkItem wi WHERE wi.assignedTo.userId = :userId AND wi.status != 'COMPLETED'")
    List<WorkItem> findActiveWorkItemsForUser(@Param("userId") Long userId);
}