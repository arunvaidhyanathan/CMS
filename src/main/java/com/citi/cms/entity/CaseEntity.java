package com.citi.cms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a Case in the Case Management System
 */
@Entity
@Table(name = "cases", schema = "cms_workflow", indexes = {
    @Index(name = "idx_case_number", columnList = "case_number", unique = true),
    @Index(name = "idx_case_id", columnList = "case_id", unique = true),
    @Index(name = "idx_case_status", columnList = "status"),
    @Index(name = "idx_case_priority", columnList = "priority"),
    @Index(name = "idx_case_created_at", columnList = "created_at"),
    @Index(name = "idx_case_assigned_to", columnList = "assigned_to_user_id"),
    @Index(name = "idx_case_created_by", columnList = "created_by_user_id"),
    @Index(name = "idx_case_type", columnList = "case_type_id"),
    @Index(name = "idx_case_department", columnList = "department_id"),
    @Index(name = "idx_workflow_instance", columnList = "workflow_instance_key")
})
@EntityListeners(AuditingEntityListener.class)
public class CaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    /**
     * Business Case ID - unique identifier for the case
     */
    @NotNull
    @Column(name = "case_id", unique = true, nullable = false)
    private Long caseId;
    
    /**
     * Human-readable case number (e.g., CMS-2024-000001)
     */
    @NotBlank
    @Size(max = 50)
    @Column(name = "case_number", unique = true, nullable = false, length = 50)
    private String caseNumber;
    
    /**
     * Case title/subject
     */
    @NotBlank
    @Size(max = 255)
    @Column(name = "title", nullable = false, length = 255)
    private String title;
    
    /**
     * Detailed case description
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    /**
     * Case type (Foreign key relationship)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_type_id", referencedColumnName = "case_type_id")
    private CaseType caseType;
    
    /**
     * Department responsible for the case
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", referencedColumnName = "department_id")
    private Department department;
    
    /**
     * Case priority level
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private Priority priority = Priority.MEDIUM;
    
    /**
     * Current case status
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CaseStatus status = CaseStatus.OPEN;
    
    /**
     * User assigned to handle this case
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id", referencedColumnName = "user_id")
    private User assignedTo;
    
    /**
     * Date when case was assigned
     */
    @Column(name = "assigned_date")
    private LocalDateTime assignedDate;
    
    /**
     * Complainant information
     */
    @Size(max = 200)
    @Column(name = "complainant_name", length = 200)
    private String complainantName;
    
    @Size(max = 255)
    @Column(name = "complainant_email", length = 255)
    private String complainantEmail;
    
    @Size(max = 20)
    @Column(name = "complainant_phone", length = 20)
    private String complainantPhone;
    
    @Column(name = "complainant_anonymous")
    private Boolean complainantAnonymous = false;
    
    /**
     * Subject/accused party information
     */
    @Size(max = 200)
    @Column(name = "subject_name", length = 200)
    private String subjectName;
    
    @Size(max = 255)
    @Column(name = "subject_email", length = 255)
    private String subjectEmail;
    
    @Size(max = 100)
    @Column(name = "subject_department", length = 100)
    private String subjectDepartment;
    
    @Size(max = 100)
    @Column(name = "subject_position", length = 100)
    private String subjectPosition;
    
    /**
     * Allegation details
     */
    @NotBlank
    @Size(max = 100)
    @Column(name = "allegation_type", nullable = false, length = 100)
    private String allegationType;
    
    @Size(max = 50)
    @Column(name = "allegation_category", length = 50)
    private String allegationCategory;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private Severity severity = Severity.MEDIUM;
    
    @Column(name = "incident_date")
    private LocalDate incidentDate;
    
    @Size(max = 200)
    @Column(name = "incident_location", length = 200)
    private String incidentLocation;
    
    /**
     * Workflow and process information
     */
    @Column(name = "workflow_instance_key")
    private Long workflowInstanceKey;
    
    @Column(name = "workflow_process_id", length = 100)
    private String workflowProcessId;
    
    @Column(name = "current_task_id", length = 100)
    private String currentTaskId;
    
    @Column(name = "current_task_name", length = 200)
    private String currentTaskName;
    
    /**
     * Classification and routing information
     */
    @Size(max = 50)
    @Column(name = "classification", length = 50)
    private String classification;
    
    @Size(max = 100)
    @Column(name = "assigned_group", length = 100)
    private String assignedGroup;
    
    @Column(name = "auto_assigned")
    private Boolean autoAssigned = false;
    
    /**
     * SLA and deadline information
     */
    @Column(name = "sla_deadline")
    private LocalDateTime slaDeadline;
    
    @Column(name = "sla_breached")
    private Boolean slaBreached = false;
    
    @Column(name = "escalation_level")
    private Integer escalationLevel = 0;
    
    @Column(name = "escalation_date")
    private LocalDateTime escalationDate;
    
    /**
     * Resolution information
     */
    @Column(name = "resolution_date")
    private LocalDateTime resolutionDate;
    
    @Column(name = "closure_date")
    private LocalDateTime closureDate;
    
    @Size(max = 50)
    @Column(name = "resolution_type", length = 50)
    private String resolutionType;
    
    @Column(name = "resolution_summary", columnDefinition = "TEXT")
    private String resolutionSummary;
    
    @Column(name = "lessons_learned", columnDefinition = "TEXT")
    private String lessonsLearned;
    
    /**
     * Investigation details
     */
    @Column(name = "investigation_required")
    private Boolean investigationRequired = true;
    
    @Column(name = "investigation_started_date")
    private LocalDateTime investigationStartedDate;
    
    @Column(name = "investigation_completed_date")
    private LocalDateTime investigationCompletedDate;
    
    @Column(name = "estimated_completion_date")
    private LocalDate estimatedCompletionDate;
    
    @Column(name = "investigation_hours")
    private Integer investigationHours = 0;
    
    @Column(name = "investigation_cost", precision = 10, scale = 2)
    private java.math.BigDecimal investigationCost;
    
    /**
     * Compliance and regulatory information
     */
    @Column(name = "regulatory_reporting_required")
    private Boolean regulatoryReportingRequired = false;
    
    @Column(name = "regulatory_reported_date")
    private LocalDateTime regulatoryReportedDate;
    
    @Size(max = 100)
    @Column(name = "regulatory_agency", length = 100)
    private String regulatoryAgency;
    
    @Size(max = 100)
    @Column(name = "external_reference_number", length = 100)
    private String externalReferenceNumber;
    
    /**
     * Risk assessment
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 20)
    private RiskLevel riskLevel = RiskLevel.MEDIUM;
    
    @Column(name = "financial_impact", precision = 12, scale = 2)
    private java.math.BigDecimal financialImpact;
    
    @Column(name = "reputational_risk")
    private Boolean reputationalRisk = false;
    
    @Column(name = "legal_risk")
    private Boolean legalRisk = false;
    
    /**
     * Confidentiality and sensitivity
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "confidentiality_level", length = 30)
    private ConfidentialityLevel confidentialityLevel = ConfidentialityLevel.INTERNAL;
    
    @Column(name = "sensitive_case")
    private Boolean sensitiveCase = false;
    
    @Column(name = "media_attention_risk")
    private Boolean mediaAttentionRisk = false;
    
    /**
     * Audit and tracking information
     */
    @NotNull
    @CreatedDate
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @NotNull
    @LastModifiedDate
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", referencedColumnName = "user_id", nullable = false, updatable = false)
    private User createdBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id", referencedColumnName = "user_id")
    private User updatedBy;
    
    @Column(name = "version", nullable = false)
    @Version
    private Long version = 0L;
    
    /**
     * Related entities
     */
    @OneToMany(mappedBy = "caseEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<WorkItem> workItems = new ArrayList<>();
    
    @OneToMany(mappedBy = "caseEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<CaseTransition> transitions = new ArrayList<>();
    
    // @OneToMany(mappedBy = "caseEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    // private List<CaseDocument> documents = new ArrayList<>();
    
    // @OneToMany(mappedBy = "caseEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    // private List<CaseNote> notes = new ArrayList<>();
    
    // @ManyToMany(fetch = FetchType.LAZY)
    // @JoinTable(
    //     name = "case_tags",
    //     joinColumns = @JoinColumn(name = "case_id", referencedColumnName = "case_id"),
    //     inverseJoinColumns = @JoinColumn(name = "tag_id", referencedColumnName = "id")
    // )
    // private Set<CaseTag> tags = new HashSet<>();
    
    // @OneToMany(mappedBy = "caseEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    // private List<CaseWitness> witnesses = new ArrayList<>();
    
    /**
     * Constructors
     */
    public CaseEntity() {}
    
    public CaseEntity(String caseNumber, String title, String description, CaseType caseType, User createdBy) {
        this.caseNumber = caseNumber;
        this.title = title;
        this.description = description;
        this.caseType = caseType;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Lifecycle Callbacks
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (version == null) {
            version = 0L;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Business Methods
     */
    
    /**
     * Check if case is overdue based on SLA
     */
    public boolean isOverdue() {
        return slaDeadline != null && LocalDateTime.now().isAfter(slaDeadline) && !isCompleted();
    }
    
    /**
     * Check if case is completed (closed, cancelled, or rejected)
     */
    public boolean isCompleted() {
        return status != null && status.isCompleted();
    }
    
    /**
     * Check if case is active (not completed)
     */
    public boolean isActive() {
        return status != null && status.isActive();
    }
    
    /**
     * Check if case requires attention
     */
    public boolean requiresAttention() {
        return (status != null && status.requiresAttention()) || isOverdue() || escalationLevel > 0;
    }
    
    /**
     * Get case age in days
     */
    public long getCaseAgeInDays() {
        if (createdAt == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(createdAt.toLocalDate(), LocalDate.now());
    }
    
    /**
     * Get investigation duration in days
     */
    public long getInvestigationDurationInDays() {
        if (investigationStartedDate == null) return 0;
        LocalDateTime endDate = investigationCompletedDate != null ? investigationCompletedDate : LocalDateTime.now();
        return java.time.temporal.ChronoUnit.DAYS.between(investigationStartedDate, endDate);
    }
    
    /**
     * Check if case has been escalated
     */
    public boolean isEscalated() {
        return escalationLevel != null && escalationLevel > 0;
    }
    
    /**
     * Get days until SLA deadline
     */
    public long getDaysUntilSLADeadline() {
        if (slaDeadline == null) return Long.MAX_VALUE;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), slaDeadline.toLocalDate());
    }
    
    /**
     * Check if case is high priority
     */
    public boolean isHighPriority() {
        return priority == Priority.HIGH || priority == Priority.CRITICAL;
    }
    
    /**
     * Check if case involves external investigation
     */
    public boolean hasExternalInvestigation() {
        return status == CaseStatus.EXTERNAL_INVESTIGATION;
    }
    
    /**
     * Get full case identifier
     */
    public String getFullCaseIdentifier() {
        return String.format("%s (ID: %d)", caseNumber, caseId);
    }
    
    /**
     * Add work item to case
     */
    /*
    public void addWorkItem(WorkItem workItem) {
        workItems.add(workItem);
        workItem.setCaseEntity(this);
    }*/
    
    /**
     * Remove work item from case
     */
    public void removeWorkItem(WorkItem workItem) {
        workItems.remove(workItem);
        workItem.setCaseEntity(null);
    }
    
    /**
     * Add case transition
     */
    /* 
    public void addTransition(CaseTransition transition) {
        transitions.add(transition);
        transition.setCaseEntity(this);
    }*/
    
    // /**
    //  * Add document to case
    //  */
    // public void addDocument(CaseDocument document) {
    //     documents.add(document);
    //     document.setCaseEntity(this);
    // }
    
    // /**
    //  * Add note to case
    //  */
    // public void addNote(CaseNote note) {
    //     notes.add(note);
    //     note.setCaseEntity(this);
    // }
    
    // /**
    //  * Add tag to case
    //  */
    // public void addTag(CaseTag tag) {
    //     tags.add(tag);
    // }
    
    // /**
    //  * Remove tag from case
    //  */
    // public void removeTag(CaseTag tag) {
    //     tags.remove(tag);
    // }
    
    // /**
    //  * Add witness to case
    //  */
    // public void addWitness(CaseWitness witness) {
    //     witnesses.add(witness);
    //     witness.setCaseEntity(this);
    // }
    
    // /**
    //  * Remove witness from case
    //  */
    // public void removeWitness(CaseWitness witness) {
    //     witnesses.remove(witness);
    //     witness.setCaseEntity(null);
    // }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    
    public String getCaseNumber() { return caseNumber; }
    public void setCaseNumber(String caseNumber) { this.caseNumber = caseNumber; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public CaseType getCaseType() { return caseType; }
    public void setCaseType(CaseType caseType) { this.caseType = caseType; }
    
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    
    public CaseStatus getStatus() { return status; }
    public void setStatus(CaseStatus status) { this.status = status; }
    
    public User getAssignedTo() { return assignedTo; }
    public void setAssignedTo(User assignedTo) { 
        this.assignedTo = assignedTo;
        if (assignedTo != null && assignedDate == null) {
            this.assignedDate = LocalDateTime.now();
        }
    }
    
    public LocalDateTime getAssignedDate() { return assignedDate; }
    public void setAssignedDate(LocalDateTime assignedDate) { this.assignedDate = assignedDate; }
    
    public String getComplainantName() { return complainantName; }
    public void setComplainantName(String complainantName) { this.complainantName = complainantName; }
    
    public String getComplainantEmail() { return complainantEmail; }
    public void setComplainantEmail(String complainantEmail) { this.complainantEmail = complainantEmail; }
    
    public String getComplainantPhone() { return complainantPhone; }
    public void setComplainantPhone(String complainantPhone) { this.complainantPhone = complainantPhone; }
    
    public Boolean getComplainantAnonymous() { return complainantAnonymous; }
    public void setComplainantAnonymous(Boolean complainantAnonymous) { this.complainantAnonymous = complainantAnonymous; }
    
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    
    public String getSubjectEmail() { return subjectEmail; }
    public void setSubjectEmail(String subjectEmail) { this.subjectEmail = subjectEmail; }
    
    public String getSubjectDepartment() { return subjectDepartment; }
    public void setSubjectDepartment(String subjectDepartment) { this.subjectDepartment = subjectDepartment; }
    
    public String getSubjectPosition() { return subjectPosition; }
    public void setSubjectPosition(String subjectPosition) { this.subjectPosition = subjectPosition; }
    
    public String getAllegationType() { return allegationType; }
    public void setAllegationType(String allegationType) { this.allegationType = allegationType; }
    
    public String getAllegationCategory() { return allegationCategory; }
    public void setAllegationCategory(String allegationCategory) { this.allegationCategory = allegationCategory; }
    
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    
    public LocalDate getIncidentDate() { return incidentDate; }
    public void setIncidentDate(LocalDate incidentDate) { this.incidentDate = incidentDate; }
    
    public String getIncidentLocation() { return incidentLocation; }
    public void setIncidentLocation(String incidentLocation) { this.incidentLocation = incidentLocation; }
    
    public Long getWorkflowInstanceKey() { return workflowInstanceKey; }
    public void setWorkflowInstanceKey(Long workflowInstanceKey) { this.workflowInstanceKey = workflowInstanceKey; }
    
    public String getWorkflowProcessId() { return workflowProcessId; }
    public void setWorkflowProcessId(String workflowProcessId) { this.workflowProcessId = workflowProcessId; }
    
    public String getCurrentTaskId() { return currentTaskId; }
    public void setCurrentTaskId(String currentTaskId) { this.currentTaskId = currentTaskId; }
    
    public String getCurrentTaskName() { return currentTaskName; }
    public void setCurrentTaskName(String currentTaskName) { this.currentTaskName = currentTaskName; }
    
    public String getClassification() { return classification; }
    public void setClassification(String classification) { this.classification = classification; }
    
    public String getAssignedGroup() { return assignedGroup; }
    public void setAssignedGroup(String assignedGroup) { this.assignedGroup = assignedGroup; }
    
    public Boolean getAutoAssigned() { return autoAssigned; }
    public void setAutoAssigned(Boolean autoAssigned) { this.autoAssigned = autoAssigned; }
    
    public LocalDateTime getSlaDeadline() { return slaDeadline; }
    public void setSlaDeadline(LocalDateTime slaDeadline) { this.slaDeadline = slaDeadline; }
    
    public Boolean getSlaBreached() { return slaBreached; }
    public void setSlaBreached(Boolean slaBreached) { this.slaBreached = slaBreached; }
    
    public Integer getEscalationLevel() { return escalationLevel; }
    public void setEscalationLevel(Integer escalationLevel) { this.escalationLevel = escalationLevel; }
    
    public LocalDateTime getEscalationDate() { return escalationDate; }
    public void setEscalationDate(LocalDateTime escalationDate) { this.escalationDate = escalationDate; }
    
    public LocalDateTime getResolutionDate() { return resolutionDate; }
    public void setResolutionDate(LocalDateTime resolutionDate) { this.resolutionDate = resolutionDate; }
    
    public LocalDateTime getClosureDate() { return closureDate; }
    public void setClosureDate(LocalDateTime closureDate) { this.closureDate = closureDate; }
    
    public String getResolutionType() { return resolutionType; }
    public void setResolutionType(String resolutionType) { this.resolutionType = resolutionType; }
    
    public String getResolutionSummary() { return resolutionSummary; }
    public void setResolutionSummary(String resolutionSummary) { this.resolutionSummary = resolutionSummary; }
    
    public String getLessonsLearned() { return lessonsLearned; }
    public void setLessonsLearned(String lessonsLearned) { this.lessonsLearned = lessonsLearned; }
    
    public Boolean getInvestigationRequired() { return investigationRequired; }
    public void setInvestigationRequired(Boolean investigationRequired) { this.investigationRequired = investigationRequired; }
    
    public LocalDateTime getInvestigationStartedDate() { return investigationStartedDate; }
    public void setInvestigationStartedDate(LocalDateTime investigationStartedDate) { this.investigationStartedDate = investigationStartedDate; }
    
    public LocalDateTime getInvestigationCompletedDate() { return investigationCompletedDate; }
    public void setInvestigationCompletedDate(LocalDateTime investigationCompletedDate) { this.investigationCompletedDate = investigationCompletedDate; }
    
    public LocalDate getEstimatedCompletionDate() { return estimatedCompletionDate; }
    public void setEstimatedCompletionDate(LocalDate estimatedCompletionDate) { this.estimatedCompletionDate = estimatedCompletionDate; }
    
    public Integer getInvestigationHours() { return investigationHours; }
    public void setInvestigationHours(Integer investigationHours) { this.investigationHours = investigationHours; }
    
    public java.math.BigDecimal getInvestigationCost() { return investigationCost; }
    public void setInvestigationCost(java.math.BigDecimal investigationCost) { this.investigationCost = investigationCost; }
    
    public Boolean getRegulatoryReportingRequired() { return regulatoryReportingRequired; }
    public void setRegulatoryReportingRequired(Boolean regulatoryReportingRequired) { this.regulatoryReportingRequired = regulatoryReportingRequired; }
    
    public LocalDateTime getRegulatoryReportedDate() { return regulatoryReportedDate; }
    public void setRegulatoryReportedDate(LocalDateTime regulatoryReportedDate) { this.regulatoryReportedDate = regulatoryReportedDate; }
    
    public String getRegulatoryAgency() { return regulatoryAgency; }
    public void setRegulatoryAgency(String regulatoryAgency) { this.regulatoryAgency = regulatoryAgency; }
    
    public String getExternalReferenceNumber() { return externalReferenceNumber; }
    public void setExternalReferenceNumber(String externalReferenceNumber) { this.externalReferenceNumber = externalReferenceNumber; }
    
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    
    public java.math.BigDecimal getFinancialImpact() { return financialImpact; }
    public void setFinancialImpact(java.math.BigDecimal financialImpact) { this.financialImpact = financialImpact; }
    
    public Boolean getReputationalRisk() { return reputationalRisk; }
    public void setReputationalRisk(Boolean reputationalRisk) { this.reputationalRisk = reputationalRisk; }
    
    public Boolean getLegalRisk() { return legalRisk; }
    public void setLegalRisk(Boolean legalRisk) { this.legalRisk = legalRisk; }
    
    public ConfidentialityLevel getConfidentialityLevel() { return confidentialityLevel; }
    public void setConfidentialityLevel(ConfidentialityLevel confidentialityLevel) { this.confidentialityLevel = confidentialityLevel; }
    
    public Boolean getSensitiveCase() { return sensitiveCase; }
    public void setSensitiveCase(Boolean sensitiveCase) { this.sensitiveCase = sensitiveCase; }
    
    public Boolean getMediaAttentionRisk() { return mediaAttentionRisk; }
    public void setMediaAttentionRisk(Boolean mediaAttentionRisk) { this.mediaAttentionRisk = mediaAttentionRisk; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    
    public User getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(User updatedBy) { this.updatedBy = updatedBy; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    
    public List<WorkItem> getWorkItems() { return workItems; }
    public void setWorkItems(List<WorkItem> workItems) { this.workItems = workItems; }
    
    public List<CaseTransition> getTransitions() { return transitions; }
    public void setTransitions(List<CaseTransition> transitions) { this.transitions = transitions; }
    
    // public List<CaseDocument> getDocuments() { return documents; }
    // public void setDocuments(List<CaseDocument> documents) { this.documents = documents; }
    
    // public List<CaseNote> getNotes() { return notes; }
    // public void setNotes(List<CaseNote> notes) { this.notes = notes; }
    
    // public Set<CaseTag> getTags() { return tags; }
    // public void setTags(Set<CaseTag> tags) { this.tags = tags; }
    
    // public List<CaseWitness> getWitnesses() { return witnesses; }
    // public void setWitnesses(List<CaseWitness> witnesses) { this.witnesses = witnesses; }
    
    /**
     * Equals and HashCode based on caseId (business key)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CaseEntity that = (CaseEntity) o;
        return caseId != null && caseId.equals(that.caseId);
    }
    
    @Override
    public int hashCode() {
        return caseId != null ? caseId.hashCode() : 0;
    }
    
    /**
     * String representation
     */
    @Override
    public String toString() {
        return String.format("CaseEntity{caseId=%d, caseNumber='%s', title='%s', status=%s, priority=%s}", 
                           caseId, caseNumber, title, status, priority);
    }
}

/**
 * Enum for Case Priority

enum Priority {
    LOW("Low", 1, "#28a745"),
    MEDIUM("Medium", 2, "#ffc107"), 
    HIGH("High", 3, "#fd7e14"),
    CRITICAL("Critical", 4, "#dc3545");
    
    private final String displayName;
    private final int level;
    private final String color;
    
    Priority(String displayName, int level, String color) {
        this.displayName = displayName;
        this.level = level;
        this.color = color;
    }
    
    public String getDisplayName() { return displayName; }
    public int getLevel() { return level; }
    public String getColor() { return color; }
    
    public boolean isHigherThan(Priority other) {
        return this.level > other.level;
    }
} */

/**
 * Enum for Case Severity
 */
enum Severity {
    MINIMAL("Minimal", 1, "#28a745"),
    LOW("Low", 2, "#17a2b8"),
    MEDIUM("Medium", 3, "#ffc107"),
    HIGH("High", 4, "#fd7e14"),
    CRITICAL("Critical", 5, "#dc3545");
    
    private final String displayName;
    private final int level;
    private final String color;
    
    Severity(String displayName, int level, String color) {
        this.displayName = displayName;
        this.level = level;
        this.color = color;
    }
    
    public String getDisplayName() { return displayName; }
    public int getLevel() { return level; }
    public String getColor() { return color; }
    
    public boolean isHigherThan(Severity other) {
        return this.level > other.level;
    }
}

/**
 * Enum for Risk Level
 */
enum RiskLevel {
    VERY_LOW("Very Low", 1, "#28a745"),
    LOW("Low", 2, "#20c997"),
    MEDIUM("Medium", 3, "#ffc107"),
    HIGH("High", 4, "#fd7e14"),
    VERY_HIGH("Very High", 5, "#dc3545");
    
    private final String displayName;
    private final int level;
    private final String color;
    
    RiskLevel(String displayName, int level, String color) {
        this.displayName = displayName;
        this.level = level;
        this.color = color;
    }
    
    public String getDisplayName() { return displayName; }
    public int getLevel() { return level; }
    public String getColor() { return color; }
    
    public boolean isHigherThan(RiskLevel other) {
        return this.level > other.level;
    }
}

/**
 * Enum for Confidentiality Level
 */
enum ConfidentialityLevel {
    PUBLIC("Public", "Available to public"),
    INTERNAL("Internal", "Internal company use only"),
    CONFIDENTIAL("Confidential", "Confidential - limited access"),
    RESTRICTED("Restricted", "Restricted - need to know basis"),
    TOP_SECRET("Top Secret", "Top Secret - highest level");
    
    private final String displayName;
    private final String description;
    
    ConfidentialityLevel(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}