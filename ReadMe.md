# Using Spring Initializr or IDE

Project: Maven Project
Language: Java 17+
Spring Boot: 3.2.x
Group: com.citi
Artifact: cms-backend
Package: jar

cms-backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── company/
│   │   │           └── cms/
│   │   │               ├── CmsApplication.java
│   │   │               ├── config/
│   │   │               │   ├── CamundaConfig.java
│   │   │               │   ├── SecurityConfig.java
│   │   │               │   ├── DatabaseConfig.java
│   │   │               │   └── WebConfig.java
│   │   │               ├── controller/
│   │   │               │   ├── AuthController.java
│   │   │               │   ├── CaseController.java
│   │   │               │   ├── WorkflowController.java
│   │   │               │   └── UserController.java
│   │   │               ├── service/
│   │   │               │   ├── AuthService.java
│   │   │               │   ├── CaseService.java
│   │   │               │   ├── WorkflowService.java
│   │   │               │   ├── UserService.java
│   │   │               │   └── impl/
│   │   │               │       ├── AuthServiceImpl.java
│   │   │               │       ├── CaseServiceImpl.java
│   │   │               │       ├── WorkflowServiceImpl.java
│   │   │               │       └── UserServiceImpl.java
│   │   │               ├── repository/
│   │   │               │   ├── UserRepository.java
│   │   │               │   ├── RoleRepository.java
│   │   │               │   ├── CaseRepository.java
│   │   │               │   ├── WorkItemRepository.java
│   │   │               │   └── CaseTransitionRepository.java
│   │   │               ├── entity/
│   │   │               │   ├── User.java
│   │   │               │   ├── Role.java
│   │   │               │   ├── UserRole.java
│   │   │               │   ├── Case.java
│   │   │               │   ├── CaseType.java
│   │   │               │   ├── Department.java
│   │   │               │   ├── WorkItem.java
│   │   │               │   └── CaseTransition.java
│   │   │               ├── dto/
│   │   │               │   ├── request/
│   │   │               │   │   ├── LoginRequest.java
│   │   │               │   │   ├── CaseCreateRequest.java
│   │   │               │   │   └── TaskCompleteRequest.java
│   │   │               │   └── response/
│   │   │               │       ├── LoginResponse.java
│   │   │               │       ├── CaseResponse.java
│   │   │               │       └── WorkItemResponse.java
│   │   │               ├── security/
│   │   │               │   ├── JwtAuthenticationEntryPoint.java
│   │   │               │   ├── JwtAuthenticationFilter.java
│   │   │               │   ├── JwtTokenProvider.java
│   │   │               │   └── UserPrincipal.java
│   │   │               ├── workflow/
│   │   │               │   ├── delegates/
│   │   │               │   │   ├── CaseCreationDelegate.java
│   │   │               │   │   ├── NotificationDelegate.java
│   │   │               │   │   └── AuditDelegate.java
│   │   │               │   └── listeners/
│   │   │               │       ├── TaskAssignmentListener.java
│   │   │               │       └── CaseStatusListener.java
│   │   │               ├── exception/
│   │   │               │   ├── GlobalExceptionHandler.java
│   │   │               │   ├── ResourceNotFoundException.java
│   │   │               │   └── UnauthorizedException.java
│   │   │               └── util/
│   │   │                   ├── Constants.java
│   │   │                   └── DateUtils.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       ├── processes/
│   │       │   ├── cms_workflow.bpmn
│   │       │   └── cms_wf.dmn
│   │       └── forms/
│   │           ├── eoIntakeForm.form
│   │           ├── hrAssignmentForm.form
│   │           ├── legalAssignmentForm.form
│   │           └── [all other forms...]
│   └── test/
│       └── java/
│           └── com/
│               └── company/
│                   └── cms/
│                       ├── CmsApplicationTests.java
│                       ├── controller/
│                       ├── service/
│                       └── repository/
├── pom.xml
└── README.md