# Using Spring Initializr or IDE

Project: Maven Project
Language: Java 17+
Spring Boot: 3.2.x
Group: com.citi
Artifact: cms-backend
Package: jar

Step 1: Get JWT Token (Authentication)
Request:
httpPOST http://localhost:8080/api/auth/login
Content-Type: application/json

{
    "username": "intake.analyst",
    "password": "demo123"
}
Expected Response:
json{
    "success": true,
    "message": "Authentication successful",
    "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJpbnRha2UuYW5hbHlzdCIsInVzZXJJZCI6MSwidGV4cCI6MTY5NTk2NzgwMH0.ABC123...",
    "user": {
        "userId": 1,
        "username": "intake.analyst",
        "email": "intake.analyst@company.com",
        "firstName": "Sarah",
        "lastName": "Johnson",
        "roles": ["INTAKE_ANALYST"]
    }
}
💡 Save the token - You'll use this in all subsequent requests in the Authorization header as Bearer {token}
Step 2: Deploy Camunda Resources (Admin Only)
Switch to admin user first:
Login as Admin:
httpPOST http://localhost:8080/api/auth/login
Content-Type: application/json

{
    "username": "admin",
    "password": "demo123"
}
Deploy BPMN Processes and DMN:
httpPOST http://localhost:8080/api/cases/processes/deploy
Authorization: Bearer {admin_token}
Expected Response:
json{
    "success": true,
    "message": "Processes deployed successfully",
    "deployedAt": "2024-01-15T10:30:00",
    "deployedBy": "admin"
}
Step 3: Create a New Case
Generate Case Number (Optional):
httpGET http://localhost:8080/api/cases/generate-case-number
Authorization: Bearer {intake_analyst_token}
Create Case:
httpPOST http://localhost:8080/api/cases
Authorization: Bearer {intake_analyst_token}
Content-Type: application/json

{
    "title": "Workplace Harassment Complaint - John Doe",
    "description": "Employee John Doe reports inappropriate behavior from supervisor during team meetings. Multiple incidents of verbal harassment and intimidation tactics reported over the past two months. Complainant requests formal investigation.",
    "caseTypeCode": "HARASSMENT",
    "allegationType": "Workplace Harassment",
    "severity": "HIGH",
    "priority": "HIGH",
    "complainantName": "John Doe",
    "complainantEmail": "john.doe@company.com"
}
Expected Response:
json{
    "id": 1,
    "caseId": 11,
    "caseNumber": "CMS-2024-000006",
    "title": "Workplace Harassment Complaint - John Doe",
    "description": "Employee John Doe reports inappropriate behavior...",
    "caseTypeName": "Workplace Harassment",
    "priority": "HIGH",
    "status": "OPEN",
    "complainantName": "John Doe",
    "complainantEmail": "john.doe@company.com",
    "createdByName": "Sarah Johnson",
    "createdAt": "2024-01-15T10:35:00",
    "updatedAt": "2024-01-15T10:35:00"
}
Step 4: Check My Work Items (As Intake Analyst)
httpGET http://localhost:8080/api/cases/my-workitems?status=PENDING
Authorization: Bearer {intake_analyst_token}
Step 5: Complete Case Tasks (Following Workflow)
5.1 Complete EO Intake Task
Find your work items first, then complete the intake task:
httpPOST http://localhost:8080/api/cases/11/complete
Authorization: Bearer {intake_analyst_token}
Content-Type: application/json

{
    "taskKey": 123456789,
    "outcome": "CLASSIFICATION_READY",
    "comments": "Initial intake completed. Case ready for classification and routing to appropriate department.",
    "variables": {
        "caseTitle": "Workplace Harassment Complaint - John Doe",
        "allegationType": "Workplace Harassment",
        "severity": "HIGH",
        "complainantName": "John Doe",
        "complainantEmail": "john.doe@company.com",
        "initialAssessment": "High priority harassment case requiring immediate HR department attention and formal investigation."
    }
}
5.2 HR Department Assignment (Login as HR Specialist)
Login as HR Specialist:
httpPOST http://localhost:8080/api/auth/login
Content-Type: application/json

{
    "username": "hr.specialist",
    "password": "demo123"
}
Check HR Work Items:
httpGET http://localhost:8080/api/cases/my-workitems?status=PENDING
Authorization: Bearer {hr_specialist_token}
Complete HR Assignment:
httpPOST http://localhost:8080/api/cases/11/complete
Authorization: Bearer {hr_specialist_token}
Content-Type: application/json

{
    "taskKey": 123456790,
    "outcome": "ASSIGNED_TO_IU",
    "comments": "HR assessment completed. Case assigned to Investigation Unit for formal investigation.",
    "variables": {
        "assignedHRSpecialist": "Michael Chen",
        "hrPriority": "HIGH",
        "hrAssessment": "Serious harassment allegations requiring formal investigation. Recommending immediate IU assignment with expedited timeline.",
        "estimatedTimelineDays": 30
    }
}
5.3 Investigation Unit Manager Assignment
Login as IU Manager:
httpPOST http://localhost:8080/api/auth/login
Content-Type: application/json

{
    "username": "iu.manager",
    "password": "demo123"
}
Complete IU Intake Assignment:
httpPOST http://localhost:8080/api/cases/11/complete
Authorization: Bearer {iu_manager_token}
Content-Type: application/json

{
    "taskKey": 123456791,
    "outcome": "TEAM_ASSIGNED",
    "comments": "Case assigned to Team Alpha for investigation.",
    "variables": {
        "investigationTeam": "TEAM_ALPHA",
        "caseComplexity": "MODERATE",
        "estimatedHours": 80,
        "targetCompletionDate": "2024-02-15T17:00:00",
        "assignmentNotes": "Priority harassment case requiring experienced investigator."
    }
}
Complete IU Processing:
httpPOST http://localhost:8080/api/cases/11/complete
Authorization: Bearer {iu_manager_token}
Content-Type: application/json

{
    "taskKey": 123456792,
    "outcome": "PROCEED_FULL",
    "comments": "Full investigation approved based on evidence availability and case severity.",
    "variables": {
        "processingDecision": "PROCEED_FULL",
        "caseFeasibility": "HIGHLY_FEASIBLE",
        "evidenceAvailability": "SUFFICIENT",
        "processingJustification": "Case meets all criteria for full investigation. Sufficient evidence available and clear policy violations alleged.",
        "recommendedApproach": "Comprehensive investigation including witness interviews, document review, and management consultation."
    }
}
Assign to Investigator:
httpPOST http://localhost:8080/api/cases/11/complete
Authorization: Bearer {iu_manager_token}
Content-Type: application/json

{
    "taskKey": 123456793,
    "outcome": "INVESTIGATOR_ASSIGNED",
    "comments": "Case assigned to senior investigator for detailed investigation.",
    "variables": {
        "primaryInvestigator": "james.wilson",
        "secondaryInvestigator": "",
        "investigationStartDate": "2024-01-16T09:00:00",
        "investigationDeadline": "2024-02-15T17:00:00",
        "allocatedBudget": 5000,
        "externalResourcesRequired": false,
        "assignmentInstructions": "Conduct thorough investigation including all witness interviews, document review, and prepare comprehensive report."
    }
}
5.4 Investigation Process (Login as Investigator)
Login as Investigator:
httpPOST http://localhost:8080/api/auth/login
Content-Type: application/json

{
    "username": "investigator",
    "password": "demo123"
}
Create Investigation Plan:
httpPOST http://localhost:8080/api/cases/11/complete
Authorization: Bearer {investigator_token}
Content-Type: application/json

{
    "taskKey": 123456794,
    "outcome": "PLAN_CREATED",
    "comments": "Comprehensive investigation plan created and ready for review.",
    "variables": {
        "planCreationDate": "2024-01-16T14:00:00",
        "investigationObjectives": "Determine validity of harassment allegations, assess policy violations, identify witnesses, collect evidence, and provide recommendations for resolution.",
        "scopeDescription": "Investigation covers period from November 2023 to January 2024, focusing on supervisor-subordinate interactions and team meeting conduct.",
        "investigationFromDate": "2023-11-01T00:00:00",
        "investigationToDate": "2024-01-15T23:59:59",
        "documentReviewRequired": true,
        "interviewsRequired": true,
        "digitalForensicsRequired": false,
        "keyPersonnelToInterview": "Complainant John Doe, Supervisor Jane Smith, Team members: Alice Brown, Bob Wilson, HR Representative Mary Davis",
        "evidenceAndDocumentation": "Team meeting recordings, email communications, HR policy documentation, previous complaint records, performance reviews",
        "estimatedDurationDays": 21,
        "keyMilestones": "Day 3: Complete document review, Day 10: Finish all interviews, Day 18: Complete evidence analysis, Day 21: Submit final report",
        "riskAssessment": "Low risk of evidence tampering. Medium risk of witness reluctance. Mitigation: early witness interviews, secure evidence collection.",
        "resourceRequirements": "Interview room booking, digital recording equipment, legal consultation for policy interpretation"
    }
}
Plan Review (Login as IU Manager again):
httpPOST http://localhost:8080/api/cases/11/complete
Authorization: Bearer {iu_manager_token}
Content-Type: application/json

{
    "taskKey": 123456795,
    "outcome": "PLAN_APPROVED",
    "comments": "Investigation plan approved. Well-structured approach with clear timeline.",
    "variables": {
        "planApproved": true,
        "planReviewDate": "2024-01-17T09:00:00",
        "objectivesClarity": "EXCELLENT",
        "scopeAppropriateness": "GOOD",
        "timelineRealism": "REALISTIC",
        "resourceAdequacy": "ADEQUATE",
        "reviewComments": "Comprehensive plan with realistic timeline. Recommend proceeding with investigation as outlined."
    }
}
Active Investigation (Login as Investigator):
httpPOST http://localhost:8080/api/cases/11/complete
Authorization: Bearer {investigator_token}
Content-Type: application/json

{
    "taskKey": 123456796,
    "outcome": "INVESTIGATION_COMPLETE",
    "comments": "Investigation completed successfully. All planned activities executed.",
    "variables": {
        "actualStartDate": "2024-01-18T09:00:00",
        "investigationStatus": "NEAR_COMPLETION",
        "documentReviewCompleted": true,
        "interviewsCompleted": true,
        "digitalForensicsCompleted": false,
        "progressSummary": "All interviews completed (5 total). Document review finished. Key evidence collected including email threads and witness statements.",
        "documentsReviewed": 25,
        "interviewsConducted": 5,
        "keyEvidenceSummary": "Multiple witnesses corroborate inappropriate behavior. Email evidence supports timeline. Supervisor acknowledges some incidents but disputes characterization.",
        "outstandingActions": "Legal review of policy violations pending",
        "challengesEncountered": "One witness initially reluctant but ultimately cooperated",
        "estimatedCompletionDate": "2024-02-05T17:00:00",
        "completionPercentage": 95
    }
}
Investigation Finalization:
httpPOST http://localhost:8080/api/cases/11/complete
Authorization: Bearer {investigator_token}
Content-Type: application/json

{
    "taskKey": 123456797,
    "outcome": "INVESTIGATION_FINALIZED",
    "comments": "Final investigation report completed with substantiated findings.",
    "variables": {
        "investigationCompletionDate": "2024-02-05T16:30:00",
        "totalInvestigationDays": 18,
        "primaryFinding": "SUBSTANTIATED",
        "confidenceLevel": "HIGH",
        "executiveSummary": "Investigation substantiated harassment allegations. Multiple witnesses confirmed inappropriate supervisor behavior including verbal harassment and intimidation tactics during team meetings over two-month period. Behavior violates company harassment policy sections 3.2 and 3.4.",
        "detailedFindings": "Evidence includes: (1) Consistent witness testimony from 4 of 5 team members, (2) Email documentation showing escalating tone, (3) Supervisor partial admission to 'firm management style', (4) Pattern of behavior targeting complainant specifically. Policy violations confirmed under sections 3.2 (verbal harassment) and 3.4 (intimidation).",
        "disciplinaryActionRecommended": true,
        "policyChangesRecommended": false,
        "recommendedActions": "1. Immediate supervisor training on respectful workplace communication, 2. Formal disciplinary action per HR policy, 3. Team environment assessment, 4. 90-day follow-up with complainant",
        "followupRequired": true,
        "followupTimelineDays": 90,
        "totalHoursSpent": 75,
        "totalCost": 4500,
        "investigationQualityRating": "EXCELLENT",
        "reviewedBy": "Robert Thompson"
    }
}
5.5 Case Closure (Login as Director)
Login as Director:
httpPOST http://localhost:8080/api/auth/login
Content-Type: application/json

{
    "username": "director",
    "password": "demo123"
}
Close Case:
httpPOST http://localhost:8080/api/cases/11/complete
Authorization: Bearer {director_token}
Content-Type: application/json

{
    "taskKey": 123456798,
    "outcome": "CASE_CLOSED",
    "comments": "Case officially closed with substantiated findings and corrective actions implemented.",
    "variables": {
        "caseClosureDate": "2024-02-08T15:00:00",
        "eoReviewDecision": "ACCEPT_FINDINGS",
        "finalCaseStatus": "CLOSED_SUBSTANTIATED",
        "disciplinaryActionImplemented": true,
        "policyChangesImplemented": false,
        "eoFinalAssessment": "Investigation was thorough and professional. Findings are well-supported by evidence. Recommended disciplinary actions are appropriate and have been implemented. Case resolution satisfactory.",
        "lessonsLearned": "Early intervention training for managers recommended. Consider quarterly team climate surveys to identify issues proactively.",
        "complainantNotified": true,
        "subjectNotified": true,
        "managementNotified": true,
        "retentionPeriodYears": 7,
        "archiveLocation": "DIGITAL_L1",
        "finalClosureComments": "Case successfully resolved with appropriate corrective measures. All stakeholders notified of resolution.",
        "overallCaseQuality": "HIGH_QUALITY",
        "processEfficiencyRating": "EFFICIENT"
    }
}
Step 6: Verification and Monitoring
Check Case Status
httpGET http://localhost:8080/api/cases/11
Authorization: Bearer {director_token}
Get Case Audit Trail
httpGET http://localhost:8080/api/cases/11/audit-trail
Authorization: Bearer {director_token}
Check Case Statistics
httpGET http://localhost:8080/api/cases/statistics?fromDate=2024-01-01&toDate=2024-02-28
Authorization: Bearer {director_token}
Key Testing Points
1. Authentication Flow

Login with different user roles
Token expiration handling
Role-based access control

2. Workflow Transitions

Case creation triggers workflow
DMN decision routing works
Task assignments to correct users
Parallel processing (HR/Legal/CSIS split)
Gateway convergence
Investigation plan approval loop

3. Data Integrity

Case data persists through transitions
Form data captured correctly
Audit trail recorded
Status updates tracked

4. User Experience

Work items appear for assigned users
Form fields populated from context
Business rules applied correctly

Troubleshooting
Common Issues:

Zeebe Connection Failed

Ensure Zeebe is running on port 26500
Check your application.yml configuration


Tasks Not Appearing

Verify process deployment was successful
Check user roles and assignments


DMN Classification Not Working

Verify DMN table deployed correctly
Check allegationType and severity values


Database Errors

Ensure PostgreSQL schema is created
Check database connection settings



Debug Commands:
bash# Check Zeebe status
docker logs zeebe

# Check your app logs
tail -f logs/application.log

# Verify process deployment in Zeebe
curl http://localhost:8088/actuator/health
Test Data Summary
Use these test credentials:

intake.analyst / demo123 (Create cases, initial processing)
hr.specialist / demo123 (HR department tasks)
legal.counsel / demo123 (Legal department tasks)
security.analyst / demo123 (CSIS department tasks)
iu.manager / demo123 (Investigation management)
investigator / demo123 (Investigation execution)
director / demo123 (Case closure and oversight)
admin / demo123 (System administration, deployment)