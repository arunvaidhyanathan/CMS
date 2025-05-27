package com.citi.cms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "work_items")
public class WorkItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "work_item_id", unique = true, nullable = false)
    private Long workItemId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", referencedColumnName = "case_id")
    private Case caseEntity;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id", referencedColumnName = "user_id")
    private User assignedTo;
    
    @NotBlank
    @Size(max = 200)
    @Column(name = "task_name")
    private String taskName;
    
    @NotBlank
    @Size(max = 100)
    @Column(name = "task_type")
    private String taskType;
    
    @Column(name = "task_key")
    private Long taskKey;
    
    @Column(name = "status")
    private String status = "PENDING";
    
    @Column(name = "due_date")
    private LocalDate dueDate;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by_user_id", referencedColumnName = "user_id")
    private User completedBy;
    
    // Constructors
    public WorkItem() {}
    
    public WorkItem(Case caseEntity, String taskName, String taskType, User assignedTo) {
        this.caseEntity = caseEntity;
        this.taskName = taskName;
        this.taskType = taskType;
        this.assignedTo = assignedTo;
        this.createdAt = LocalDateTime.now();
    }
    
    // Lifecycle Callbacks
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getWorkItemId() { return workItemId; }
    public void setWorkItemId(Long workItemId) { this.workItemId = workItemId; }
    
    public Case getCaseEntity() { return caseEntity; }
    
    public void setCaseEntity(Case caseEntity) {
        this.caseEntity = caseEntity;
    }
    
    public User getAssignedTo() { return assignedTo; }
    public void setAssignedTo(User assignedTo) { this.assignedTo = assignedTo; }
    
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    
    public Long getTaskKey() { return taskKey; }
    public void setTaskKey(Long taskKey) { this.taskKey = taskKey; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public User getCompletedBy() { return completedBy; }
    public void setCompletedBy(User completedBy) { this.completedBy = completedBy; }
}