package com.citi.cms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CaseCreateRequest {
    @NotBlank
    @Size(max = 255)
    private String title;
    
    private String description;
    
    @NotBlank
    private String caseTypeCode;
    
    @NotBlank
    private String allegationType;
    
    @NotBlank
    private String severity;
    
    @NotBlank
    private String priority;
    
    private String complainantName;
    private String complainantEmail;
    
    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCaseTypeCode() { return caseTypeCode; }
    public void setCaseTypeCode(String caseTypeCode) { this.caseTypeCode = caseTypeCode; }
    
    public String getAllegationType() { return allegationType; }
    public void setAllegationType(String allegationType) { this.allegationType = allegationType; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    
    public String getComplainantName() { return complainantName; }
    public void setComplainantName(String complainantName) { this.complainantName = complainantName; }
    
    public String getComplainantEmail() { return complainantEmail; }
    public void setComplainantEmail(String complainantEmail) { this.complainantEmail = complainantEmail; }
}
