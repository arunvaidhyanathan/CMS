-- Schema Creation

--CREATE SCHEMA cms_workflow AUTHORIZATION <database Name>;

-- Run this BEFORE the main Table Creation

-- 1. CREATE SEQUENCES FOR BUSINESS IDs


-- Users sequence
CREATE SEQUENCE IF NOT EXISTS cms_workflow.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Roles sequence
CREATE SEQUENCE IF NOT EXISTS cms_workflow.roles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- User roles sequence
CREATE SEQUENCE IF NOT EXISTS cms_workflow.user_roles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Case types sequence
CREATE SEQUENCE IF NOT EXISTS cms_workflow.case_types_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Departments sequence
CREATE SEQUENCE IF NOT EXISTS cms_workflow.departments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Cases sequence
CREATE SEQUENCE IF NOT EXISTS cms_workflow.cases_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Work items sequence
CREATE SEQUENCE IF NOT EXISTS cms_workflow.work_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- 2. CASE NUMBER GENERATION SEQUENCES (BY YEAR)

-- Case numbering sequences for different years
CREATE SEQUENCE IF NOT EXISTS cms_workflow.case_number_2024_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS cms_workflow.case_number_2025_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS cms_workflow.case_number_2026_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


-- 3. FUNCTION TO GENERATE CASE NUMBERS


CREATE OR REPLACE FUNCTION cms_workflow.generate_case_number(case_year INTEGER DEFAULT EXTRACT(YEAR FROM CURRENT_DATE))
RETURNS VARCHAR(50) AS $$
DECLARE
    sequence_name TEXT;
    next_val INTEGER;
    case_num VARCHAR(50);
BEGIN
    -- Construct sequence name based on year
    sequence_name := 'case_number_' || case_year::TEXT || '_seq';
    
    -- Create sequence if it doesn't exist
    EXECUTE format('CREATE SEQUENCE IF NOT EXISTS %I START 1', sequence_name);
    
    -- Get next value from year-specific sequence
    EXECUTE format('SELECT nextval(''%I'')', sequence_name) INTO next_val;
    
    -- Format: CMS-YYYY-NNNNNN
    case_num := 'CMS-' || case_year::TEXT || '-' || LPAD(next_val::TEXT, 6, '0');
    
    RETURN case_num;
END;
$$ LANGUAGE plpgsql;


-- 4. FUNCTION TO GET NEXT BUSINESS ID


CREATE OR REPLACE FUNCTION cms_workflow.get_next_business_id(table_name TEXT)
RETURNS INTEGER AS $$
DECLARE
    sequence_name TEXT;
    next_val INTEGER;
BEGIN
    sequence_name := table_name || '_id_seq';
    EXECUTE format('SELECT nextval(''%I'')', sequence_name) INTO next_val;
    RETURN next_val;
END;
$$ LANGUAGE plpgsql;


-- 5. RESET SEQUENCES TO CURRENT MAX VALUES (OPTIONAL)


-- Function to reset sequence to current max value in table
CREATE OR REPLACE FUNCTION cms_workflow.reset_sequence_to_max(table_name TEXT, id_column TEXT, sequence_name TEXT)
RETURNS VOID AS $$
DECLARE
    max_val INTEGER;
BEGIN
    EXECUTE format('SELECT COALESCE(MAX(%I), 0) FROM %I', id_column, table_name) INTO max_val;
    EXECUTE format('SELECT setval(''%I'', %s)', sequence_name, max_val + 1);
END;
$$ LANGUAGE plpgsql;


-- 6. INITIALIZE SEQUENCES WITH STARTING VALUES


-- Set starting values for business ID sequences
SELECT setval('cms_workflow.users_id_seq', 10);          -- Start user IDs from 10
SELECT setval('cms_workflow.roles_id_seq', 10);          -- Start role IDs from 10  
SELECT setval('cms_workflow.user_roles_id_seq', 20);     -- Start user-role mappings from 20
SELECT setval('cms_workflow.case_types_id_seq', 10);     -- Start case type IDs from 10
SELECT setval('cms_workflow.departments_id_seq', 10);    -- Start department IDs from 10
SELECT setval('cms_workflow.cases_id_seq', 10);          -- Start case IDs from 10
SELECT setval('cms_workflow.work_items_id_seq', 10);     -- Start work item IDs from 10

-- Set case numbering sequences
SELECT setval('cms_workflow.case_number_2024_seq', 5);   -- We have 5 sample cases for 2024
SELECT setval('cms_workflow.case_number_2025_seq', 1);   -- Start 2025 cases from 1

-- 7. SEQUENCE MONITORING FUNCTIONS


-- Function to check all sequence values
CREATE OR REPLACE FUNCTION cms_workflow.check_sequence_values()
RETURNS TABLE (
    sequence_name TEXT,
    current_value BIGINT,
    is_called BOOLEAN
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        s.sequencename::TEXT,
        s.last_value,
        s.is_called
    FROM pg_sequences s
    WHERE s.schemaname = 'public'
    AND s.sequencename LIKE '%_seq'
    ORDER BY s.sequencename;
END;
$$ LANGUAGE plpgsql;

-- Function to generate sample case numbers for testing
CREATE OR REPLACE FUNCTION cms_workflow.generate_sample_case_numbers(count_per_year INTEGER DEFAULT 3)
RETURNS TABLE (
    year INTEGER,
    case_number VARCHAR(50)
) AS $$
DECLARE
    current_year INTEGER := EXTRACT(YEAR FROM CURRENT_DATE);
    i INTEGER;
BEGIN
    -- Generate case numbers for current year
    FOR i IN 1..count_per_year LOOP
        year := current_year;
        case_number := generate_case_number(current_year);
        RETURN NEXT;
    END LOOP;
    
    -- Generate case numbers for next year
    FOR i IN 1..count_per_year LOOP
        year := current_year + 1;
        case_number := generate_case_number(current_year + 1);
        RETURN NEXT;
    END LOOP;
END;
$$ LANGUAGE plpgsql;


-- 8. USAGE EXAMPLES AND TESTING


-- Test case number generation
/*
SELECT generate_case_number(); -- Uses current year
SELECT generate_case_number(2024);
SELECT generate_case_number(2025);
*/

-- Test business ID generation
/*
SELECT get_next_business_id('users');
SELECT get_next_business_id('cases');
*/

-- Check sequence values
/*
SELECT * FROM check_sequence_values();
*/

-- Generate sample case numbers
/*
SELECT * FROM generate_sample_case_numbers(3);
*/


-- 9. CLEANUP FUNCTIONS


-- Function to reset all sequences to 1 (USE WITH CAUTION!)
CREATE OR REPLACE FUNCTION cms_workflow.reset_all_sequences()
RETURNS VOID AS $$
DECLARE
    seq_record RECORD;
BEGIN
    FOR seq_record IN 
        SELECT sequencename 
        FROM pg_sequences 
        WHERE schemaname = 'public' 
        AND sequencename LIKE '%_seq'
    LOOP
        EXECUTE format('SELECT setval(''%I'', 1, false)', seq_record.sequencename);
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- Quick verification query
SELECT 
    'Sequences created successfully!' as status,
    COUNT(*) as total_sequences
FROM pg_sequences 
WHERE schemaname = 'cms_workflow' 
AND sequencename LIKE '%_seq';

-- 1. ROLES MASTER TABLE

CREATE TABLE cms_workflow.roles (
    id SERIAL PRIMARY KEY,
    role_id INTEGER UNIQUE NOT NULL DEFAULT nextval('cms_workflow.roles_id_seq'), -- Business ID
    role_code VARCHAR(50) UNIQUE NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    role_description TEXT,
    access_level VARCHAR(20) DEFAULT 'USER', -- ADMIN, MANAGER, USER, VIEWER
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);


-- 2. USERS MASTER TABLE

CREATE TABLE cms_workflow.users (
    id SERIAL PRIMARY KEY,
    user_id INTEGER UNIQUE NOT NULL DEFAULT nextval('cms_workflow.users_id_seq'), -- Business ID
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    user_status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, SUSPENDED
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);


-- 3. CASE TYPES MASTER TABLE

CREATE TABLE cms_workflow.case_types (
    id SERIAL PRIMARY KEY,
    case_type_id INTEGER UNIQUE NOT NULL DEFAULT nextval('cms_workflow.case_types_id_seq'), -- Business ID
    type_code VARCHAR(50) UNIQUE NOT NULL,
    type_name VARCHAR(100) NOT NULL,
    type_description TEXT,
    default_priority VARCHAR(20) DEFAULT 'MEDIUM',
    sla_hours INTEGER DEFAULT 72,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);


-- 4. DEPARTMENTS MASTER TABLE

CREATE TABLE cms_workflow.departments (
    id SERIAL PRIMARY KEY,
    department_id INTEGER UNIQUE NOT NULL DEFAULT nextval('cms_workflow.departments_id_seq'), -- Business ID
    department_code VARCHAR(50) UNIQUE NOT NULL,
    department_name VARCHAR(100) NOT NULL,
    department_description TEXT,
    manager_user_id INTEGER, -- Will reference users.user_id
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);


-- 5. USER ROLES MAPPING TABLE

CREATE TABLE cms_workflow.user_roles (
    id SERIAL PRIMARY KEY,
    user_role_id INTEGER UNIQUE NOT NULL DEFAULT nextval('cms_workflow.user_roles_id_seq'), -- Business ID
    user_id INTEGER NOT NULL, -- References users.user_id
    role_id INTEGER NOT NULL, -- References roles.role_id
    assigned_date DATE DEFAULT CURRENT_DATE,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, role_id)
);


-- 6. CASES TABLE (SIMPLIFIED)

CREATE TABLE cms_workflow.cases (
    id SERIAL PRIMARY KEY,
    case_id INTEGER UNIQUE NOT NULL DEFAULT nextval('cms_workflow.cases_id_seq'), -- Business ID
    case_number VARCHAR(50) UNIQUE NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    case_type_id INTEGER, -- References case_types.case_type_id
    department_id INTEGER, -- References departments.department_id
    priority VARCHAR(20) DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH, CRITICAL
    status VARCHAR(50) DEFAULT 'OPEN', -- OPEN, IN_PROGRESS, RESOLVED, CLOSED
    assigned_to_user_id INTEGER, -- References users.user_id
    complainant_name VARCHAR(200),
    complainant_email VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by_user_id INTEGER -- References users.user_id
);


-- 7. WORK ITEMS TABLE (FOR TASKLIST)

CREATE TABLE cms_workflow.work_items (
    id SERIAL PRIMARY KEY,
    work_item_id INTEGER UNIQUE NOT NULL DEFAULT nextval('cms_workflow.work_items_id_seq'), -- Business ID
    case_id INTEGER NOT NULL, -- References cases.case_id
    assigned_to_user_id INTEGER NOT NULL, -- References users.user_id
    task_name VARCHAR(200) NOT NULL,
    task_type VARCHAR(100) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, IN_PROGRESS, COMPLETED
    due_date DATE,
    created_at TIMESTAMP DEFAULT NOW(),
    completed_at TIMESTAMP,
    completed_by_user_id INTEGER -- References users.user_id
);

CREATE TABLE IF NOT EXISTS cms_workflow.case_transitions (
id BIGSERIAL PRIMARY KEY,
transition_id BIGINT UNIQUE,
case_id BIGINT NOT NULL,
from_status VARCHAR(30),
to_status VARCHAR(30),
task_definition_key VARCHAR(255),
task_name VARCHAR(255),
workflow_instance_key BIGINT,
task_key BIGINT,
performed_by_user_id BIGINT,
transition_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
comments TEXT,
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
-- Foreign key constraints
CONSTRAINT fk_case_transitions_case_id 
    FOREIGN KEY (case_id) REFERENCES cms_workflow.cases(case_id) ON DELETE CASCADE,
CONSTRAINT fk_case_transitions_user_id 
    FOREIGN KEY (performed_by_user_id) REFERENCES cms_workflow.users(user_id) ON DELETE SET NULL
);
-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_case_transitions_case_id ON cms_workflow.case_transitions(case_id);
CREATE INDEX IF NOT EXISTS idx_case_transitions_transition_date ON cms_workflow.case_transitions(transition_date);
CREATE INDEX IF NOT EXISTS idx_case_transitions_task_key ON cms_workflow.case_transitions(task_key);
CREATE INDEX IF NOT EXISTS idx_case_transitions_workflow_instance ON cms_workflow.case_transitions(workflow_instance_key);

-- CREATE transition_variables TABLE (for @ElementCollection)

CREATE TABLE IF NOT EXISTS cms_workflow.transition_variables (
case_transition_id BIGINT NOT NULL,
variable_name VARCHAR(255) NOT NULL,
variable_value TEXT,
PRIMARY KEY (case_transition_id, variable_name),
CONSTRAINT fk_transition_variables_case_transition 
    FOREIGN KEY (case_transition_id) REFERENCES cms_workflow.case_transitions(id) ON DELETE CASCADE
);


-- 8. ADD FOREIGN KEY CONSTRAINTS AFTER ALL TABLES CREATED


-- User roles references
ALTER TABLE cms_workflow.user_roles 
ADD CONSTRAINT fk_user_roles_user_id 
FOREIGN KEY (user_id) REFERENCES cms_workflow.users(user_id) ON DELETE CASCADE;

ALTER TABLE cms_workflow.user_roles 
ADD CONSTRAINT fk_user_roles_role_id 
FOREIGN KEY (role_id) REFERENCES cms_workflow.roles(role_id) ON DELETE CASCADE;

-- Departments manager reference
ALTER TABLE cms_workflow.departments 
ADD CONSTRAINT fk_departments_manager_user_id 
FOREIGN KEY (manager_user_id) REFERENCES cms_workflow.users(user_id) ON DELETE SET NULL;

-- Cases references
ALTER TABLE cms_workflow.cases 
ADD CONSTRAINT fk_cases_case_type_id 
FOREIGN KEY (case_type_id) REFERENCES cms_workflow.case_types(case_type_id) ON DELETE SET NULL;

ALTER TABLE cms_workflow.cases 
ADD CONSTRAINT fk_cases_department_id 
FOREIGN KEY (department_id) REFERENCES cms_workflow.departments(department_id) ON DELETE SET NULL;

ALTER TABLE cms_workflow.cases 
ADD CONSTRAINT fk_cases_assigned_to_user_id 
FOREIGN KEY (assigned_to_user_id) REFERENCES cms_workflow.users(user_id) ON DELETE SET NULL;

ALTER TABLE cms_workflow.cases 
ADD CONSTRAINT fk_cases_created_by_user_id 
FOREIGN KEY (created_by_user_id) REFERENCES cms_workflow.users(user_id) ON DELETE SET NULL;

-- Work items references
ALTER TABLE cms_workflow.work_items 
ADD CONSTRAINT fk_work_items_case_id 
FOREIGN KEY (case_id) REFERENCES cms_workflow.cases(case_id) ON DELETE CASCADE;

ALTER TABLE cms_workflow.work_items 
ADD CONSTRAINT fk_work_items_assigned_to_user_id 
FOREIGN KEY (assigned_to_user_id) REFERENCES cms_workflow.users(user_id) ON DELETE CASCADE;

ALTER TABLE cms_workflow.work_items 
ADD CONSTRAINT fk_work_items_completed_by_user_id 
FOREIGN KEY (completed_by_user_id) REFERENCES cms_workflow.users(user_id) ON DELETE SET NULL;


-- 9. INDEXES FOR PERFORMANCE

CREATE INDEX idx_users_username ON cms_workflow.users(username);
CREATE INDEX idx_users_email ON cms_workflow.users(email);
CREATE INDEX idx_users_user_id ON cms_workflow.users(user_id);
CREATE INDEX idx_roles_role_id ON cms_workflow.roles(role_id);
CREATE INDEX idx_cases_status ON cms_workflow.cases(status);
CREATE INDEX idx_cases_assigned_to ON cms_workflow.cases(assigned_to_user_id);
CREATE INDEX idx_cases_case_id ON cms_workflow.cases(case_id);
CREATE INDEX idx_work_items_assigned_to ON cms_workflow.work_items(assigned_to_user_id);
CREATE INDEX idx_work_items_status ON cms_workflow.work_items(status);
CREATE INDEX idx_work_items_case_id ON cms_workflow.work_items(case_id);

-- 10. INSERT MASTER DATA - CASE TYPES

INSERT INTO cms_workflow.case_types (case_type_id, type_code, type_name, type_description, default_priority, sla_hours) VALUES
(1, 'HARASSMENT', 'Workplace Harassment', 'Cases involving harassment, bullying, or inappropriate workplace behavior', 'HIGH', 48),
(2, 'DISCRIMINATION', 'Discrimination', 'Cases involving discrimination based on protected characteristics', 'HIGH', 72),
(3, 'FRAUD', 'Financial Fraud', 'Cases involving financial misconduct, embezzlement, or fraud', 'CRITICAL', 24),
(4, 'SECURITY', 'Security Incident', 'Data breaches, security violations, and cyber incidents', 'CRITICAL', 12),
(5, 'POLICY', 'Policy Violation', 'Violations of company policies and code of conduct', 'MEDIUM', 120),
(6, 'COMPLIANCE', 'Compliance Violation', 'Regulatory and legal compliance issues', 'HIGH', 48);


-- 11. INSERT MASTER DATA - DEPARTMENTS

INSERT INTO cms_workflow.departments (department_id, department_code, department_name, department_description) VALUES
(1, 'HR', 'Human Resources', 'Handles employee relations, harassment, and HR policy violations'),
(2, 'LEGAL', 'Legal Department', 'Manages legal matters, compliance, and regulatory issues'), 
(3, 'SECURITY', 'Corporate Security', 'Investigates security incidents and data breaches'),
(4, 'ETHICS', 'Ethics Office', 'Oversees ethical conduct and integrity matters'),
(5, 'FINANCE', 'Finance Department', 'Handles financial fraud and monetary misconduct'),
(6, 'IT', 'Information Technology', 'Manages technical security and system incidents');


-- 12. INSERT MASTER DATA - ROLES

INSERT INTO cms_workflow.roles (role_id, role_code, role_name, role_description, access_level) VALUES
(1, 'INTAKE_ANALYST', 'Intake Analyst', 'Initial case processing and triage. Reviews incoming cases and routes to appropriate departments.', 'USER'),
(2, 'HR_SPECIALIST', 'HR Specialist', 'Handles HR-related cases including harassment, discrimination, and policy violations.', 'USER'),
(3, 'LEGAL_COUNSEL', 'Legal Counsel', 'Manages legal cases including fraud, compliance violations, and regulatory matters.', 'MANAGER'),
(4, 'SECURITY_ANALYST', 'Security Analyst', 'Investigates security breaches, data incidents, and physical security matters.', 'USER'),
(5, 'INVESTIGATOR', 'Investigator', 'Conducts detailed investigations, interviews, and evidence collection.', 'USER'),
(6, 'DIRECTOR', 'Director/Chief Ethics Officer', 'Final case approval, policy decisions, and organizational oversight.', 'ADMIN'),
(7, 'IU_MANAGER', 'Investigation Unit Manager', 'Manages investigation team, assigns cases, and reviews investigation plans.', 'MANAGER'),
(8, 'ADMIN', 'System Administrator', 'Full system access for configuration and user management.', 'ADMIN');


-- 13. INSERT TEST USERS (Based on Recommended Test Credentials)

INSERT INTO cms_workflow.users (user_id, username, email, password_hash, first_name, last_name, user_status) VALUES
(1, 'intake.analyst', 'intake.analyst@company.com', '$2a$10$demo.hash.for.demo123', 'Sarah', 'Johnson', 'ACTIVE'),
(2, 'hr.specialist', 'hr.specialist@company.com', '$2a$10$demo.hash.for.demo123', 'Michael', 'Chen', 'ACTIVE'),
(3, 'legal.counsel', 'legal.counsel@company.com', '$2a$10$demo.hash.for.demo123', 'Amanda', 'Wilson', 'ACTIVE'),
(4, 'security.analyst', 'security.analyst@company.com', '$2a$10$demo.hash.for.demo123', 'Alex', 'Turner', 'ACTIVE'),
(5, 'investigator', 'investigator@company.com', '$2a$10$demo.hash.for.demo123', 'John', 'Davis', 'ACTIVE'),
(6, 'director', 'director@company.com', '$2a$10$demo.hash.for.demo123', 'Elizabeth', 'Martinez', 'ACTIVE'),
(7, 'iu.manager', 'iu.manager@company.com', '$2a$10$demo.hash.for.demo123', 'Robert', 'Thompson', 'ACTIVE'),
(8, 'admin', 'admin@company.com', '$2a$10$demo.hash.for.demo123', 'System', 'Administrator', 'ACTIVE');


-- 14. ASSIGN ROLES TO USERS

INSERT INTO cms_workflow.user_roles (user_role_id, user_id, role_id) VALUES
(1, 1, 1), -- Intake Analyst
(2, 2, 2), -- HR Specialist  
(3, 3, 3), -- Legal Counsel
(4, 4, 4), -- Security Analyst
(5, 5, 5), -- Investigator
(6, 6, 6), -- Director
(7, 6, 7), -- Director also IU Manager for demo
(8, 7, 7), -- IU Manager
(9, 8, 8); -- Admin


-- 15. UPDATE DEPARTMENT MANAGERS

UPDATE cms_workflow.departments SET manager_user_id = 2 WHERE department_id = 1; -- HR managed by HR Specialist
UPDATE cms_workflow.departments SET manager_user_id = 3 WHERE department_id = 2; -- Legal managed by Legal Counsel
UPDATE cms_workflow.departments SET manager_user_id = 4 WHERE department_id = 3; -- Security managed by Security Analyst
UPDATE cms_workflow.departments SET manager_user_id = 6 WHERE department_id = 4; -- Ethics managed by Director


-- 16. SAMPLE CASE DATA FOR DEMO

INSERT INTO cms_workflow.cases (case_id, case_number, title, description, case_type_id, department_id, priority, status, assigned_to_user_id, complainant_name, complainant_email, created_by_user_id) VALUES
(1, 'CMS-2024-000001', 'Workplace Harassment Complaint', 'Employee reports inappropriate behavior from supervisor during team meetings.', 1, 1, 'HIGH', 'OPEN', 2, 'Jane Smith', 'jane.smith@company.com', 1),
(2, 'CMS-2024-000002', 'Data Security Incident', 'Unauthorized access detected in customer database system.', 4, 3, 'CRITICAL', 'IN_PROGRESS', 4, 'IT Security Team', 'security@company.com', 1),
(3, 'CMS-2024-000003', 'Financial Fraud Investigation', 'Suspicious expense reports submitted by department manager.', 3, 2, 'HIGH', 'OPEN', 3, 'Anonymous', 'hotline@company.com', 1),
(4, 'CMS-2024-000004', 'Discrimination Complaint', 'Employee alleges age discrimination in promotion decisions.', 2, 1, 'MEDIUM', 'OPEN', 2, 'Robert Johnson', 'robert.j@company.com', 1),
(5, 'CMS-2024-000005', 'Policy Violation Report', 'Multiple violations of company code of conduct reported.', 5, 1, 'MEDIUM', 'RESOLVED', 2, 'Manager Review', 'manager@company.com', 1);


-- 17. SAMPLE WORK ITEMS FOR DEMO

INSERT INTO cms_workflow.work_items (work_item_id, case_id, assigned_to_user_id, task_name, task_type, status, due_date) VALUES
(1, 1, 2, 'Review Harassment Complaint', 'EO Assignment - HR', 'PENDING', CURRENT_DATE + INTERVAL '3 days'),
(2, 2, 4, 'Investigate Security Breach', 'EO Assignment - CSIS', 'IN_PROGRESS', CURRENT_DATE + INTERVAL '1 day'),
(3, 3, 3, 'Legal Review - Fraud Case', 'EO Assignment - Legal', 'PENDING', CURRENT_DATE + INTERVAL '5 days'),
(4, 4, 2, 'Discrimination Case Assessment', 'EO Assignment - HR', 'PENDING', CURRENT_DATE + INTERVAL '7 days'),
(5, 1, 5, 'Conduct Initial Investigation', 'IU Investigation - Investigation Plan Creation', 'PENDING', CURRENT_DATE + INTERVAL '10 days');

-- Option 1: Update Database Schema (Recommended for Production)
-- Convert SERIAL columns to BIGSERIAL to match your Long entities

-- For case_types table
ALTER TABLE cms_workflow.case_types 
ALTER COLUMN id TYPE BIGINT;

-- Update the sequence to use BIGINT
ALTER SEQUENCE cms_workflow.case_types_id_seq AS BIGINT;

-- For cases table  
ALTER TABLE cms_workflow.cases 
ALTER COLUMN id TYPE BIGINT;

ALTER SEQUENCE cms_workflow.cases_id_seq AS BIGINT;

-- For all other tables with SERIAL id columns, repeat the pattern:
-- ALTER TABLE cms_workflow.{table_name} ALTER COLUMN id TYPE BIGINT;
-- ALTER SEQUENCE cms_workflow.{table_name}_id_seq AS BIGINT;

-- List of tables that likely need this fix:
-- case_types, cases, case_transitions, departments, roles, users, user_roles, work_items

-- Complete script for all tables:
ALTER TABLE cms_workflow.case_types ALTER COLUMN id TYPE BIGINT;
ALTER SEQUENCE cms_workflow.case_types_id_seq AS BIGINT;

ALTER TABLE cms_workflow.cases ALTER COLUMN id TYPE BIGINT;
ALTER SEQUENCE cms_workflow.cases_id_seq AS BIGINT;

ALTER TABLE cms_workflow.case_transitions ALTER COLUMN id TYPE BIGINT;
ALTER SEQUENCE cms_workflow.case_transitions_id_seq AS BIGINT;

ALTER TABLE cms_workflow.departments ALTER COLUMN id TYPE BIGINT;
ALTER SEQUENCE cms_workflow.departments_id_seq AS BIGINT;

ALTER TABLE cms_workflow.roles ALTER COLUMN id TYPE BIGINT;
ALTER SEQUENCE cms_workflow.roles_id_seq AS BIGINT;

ALTER TABLE cms_workflow.users ALTER COLUMN id TYPE BIGINT;
ALTER SEQUENCE cms_workflow.users_id_seq AS BIGINT;

ALTER TABLE cms_workflow.user_roles ALTER COLUMN id TYPE BIGINT;
ALTER SEQUENCE cms_workflow.user_roles_id_seq AS BIGINT;

ALTER TABLE cms_workflow.work_items ALTER COLUMN id TYPE BIGINT;
ALTER SEQUENCE cms_workflow.work_items_id_seq AS BIGINT;

-- 13. USEFUL QUERIES FOR DEMO

-- Query to get user with roles
/*
SELECT 
    u.username,
    u.first_name,
    u.last_name,
    u.email,
    r.role_name,
    r.role_description
FROM users u
JOIN user_roles ur ON u.user_id = ur.user_id
JOIN roles r ON ur.role_id = r.role_id
WHERE u.user_status = 'ACTIVE'
ORDER BY u.username;
*/

-- Query to get work items for a user
/*
SELECT 
    wi.task_name,
    wi.task_type,
    wi.status,
    wi.due_date,
    c.case_number,
    c.title,
    c.priority
FROM work_items wi
JOIN cases c ON wi.case_id = c.case_id
WHERE wi.assigned_to = ? -- Replace with user_id
AND wi.status != 'COMPLETED'
ORDER BY wi.due_date;
*/

-- Query to get case details
/*
SELECT 
    c.*,
    u.first_name || ' ' || u.last_name as assigned_to_name,
    creator.first_name || ' ' || creator.last_name as created_by_name
FROM cases c
LEFT JOIN users u ON c.assigned_to = u.user_id
LEFT JOIN users creator ON c.created_by = creator.user_id
WHERE c.case_id = ?; -- Replace with case_id
*/


-- 14. DEMO LOGIN CREDENTIALS SUMMARY


/*
DEMO LOGIN CREDENTIALS (Password: demo123 for all users):

1. Username: intake.analyst
   Role: Intake Analyst
   Purpose: Initial case processing and routing

2. Username: hr.specialist  
   Role: HR Specialist
   Purpose: Handle HR-related cases

3. Username: legal.counsel
   Role: Legal Counsel
   Purpose: Manage legal and compliance cases

4. Username: security.analyst
   Role: Security Analyst  
   Purpose: Investigate security incidents

5. Username: investigator
   Role: Investigator
   Purpose: Conduct detailed investigations

6. Username: director
   Role: Director/Chief Ethics Officer
   Purpose: Final approvals and oversight

7. Username: iu.manager
   Role: Investigation Unit Manager
   Purpose: Manage investigation team

8. Username: admin
   Role: System Administrator
   Purpose: Full system access
*/

SELECT 
    table_name,
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns 
WHERE table_schema = 'cms_workflow' 
    AND data_type IN ('integer', 'bigint', 'serial', 'bigserial')
ORDER BY table_name, column_name;


-- Check sequences and their data types
SELECT 
    schemaname,
    sequencename,
    data_type,
    start_value,
    min_value,
    max_value
FROM pg_sequences 
WHERE schemaname = 'cms_workflow';

-- Check foreign key relationships that might be affected
SELECT
    tc.table_name, 
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name 
FROM information_schema.table_constraints AS tc 
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
    AND tc.table_schema = kcu.table_schema
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
    AND ccu.table_schema = tc.table_schema
WHERE tc.constraint_type = 'FOREIGN KEY' 
    AND tc.table_schema = 'cms_workflow';

-- Connect to your database and run:
ALTER TABLE cms_workflow.case_types ALTER COLUMN id TYPE BIGINT;
ALTER SEQUENCE cms_workflow.case_types_id_seq AS BIGINT;

UPDATE cms_workflow.users 
SET password_hash = '$2a$10$N.zmdr9k7uOsaLQJeuOISOXzDUz5vbMRoATWY4EABP/CL/8AUed0O'
WHERE password_hash = '$2a$10$demo.hash.for.demo123';


-- Quick SQL fix for user passwords
-- Connect to your PostgreSQL database and run these commands

-- 1. Check current password hashes
SELECT username, password_hash, 
       CASE WHEN password_hash = '$2a$10$demo.hash.for.demo123' 
            THEN 'NEEDS UPDATE' 
            ELSE 'OK' 
       END as status
FROM cms_workflow.users
ORDER BY username;

-- 2. Update all users with proper BCrypt hash for "demo123"
-- This hash was generated using BCryptPasswordEncoder with strength 10
UPDATE cms_workflow.users 
SET password_hash = '$2a$10$N.zmdr9k7uOsaLQJeuOISOXzDUz5vbMRoATWY4EABP/CL/8AUed0O'
WHERE password_hash = '$2a$10$demo.hash.for.demo123';

-- 3. Verify the update
SELECT username, email, first_name, last_name,
       CASE WHEN password_hash = '$2a$10$N.zmdr9k7uOsaLQJeuOISOXzDUz5vbMRoATWY4EABP/CL/8AUed0O' 
            THEN '✅ PASSWORD UPDATED - Use: demo123' 
            ELSE '❌ PASSWORD NOT UPDATED' 
       END as login_status
FROM cms_workflow.users
ORDER BY username;


-- STEP 1: Check current password hashes
SELECT username, 
       password_hash,
       CASE 
           WHEN password_hash = '$2a$10$demo.hash.for.demo123' THEN 'NEEDS UPDATE ❌'
           WHEN password_hash = '$2a$10$N.zmdr9k7uOsaLQJeuOISOXzDUz5vbMRoATWY4EABP/CL/8AUed0O' THEN 'CORRECT HASH ✅'
           ELSE 'UNKNOWN HASH ❓'
       END as status
FROM cms_workflow.users 
ORDER BY username;

-- STEP 2: Update all users with correct BCrypt hash for "demo123"
UPDATE cms_workflow.users 
SET password_hash = '$2a$10$N.zmdr9k7uOsaLQJeuOISOXzDUz5vbMRoATWY4EABP/CL/8AUed0O';

-- STEP 3: Verify the update worked
SELECT username, first_name, last_name, email,
       CASE 
           WHEN password_hash = '$2a$10$N.zmdr9k7uOsaLQJeuOISOXzDUz5vbMRoATWY4EABP/CL/8AUed0O' 
           THEN '✅ Password: demo123' 
           ELSE '❌ Password not updated' 
       END as login_info
FROM cms_workflow.users 
ORDER BY username;

-- RESULT: All users should now be able to login with password "demo123"

-- 4. Alternative: Update individual users if needed
-- UPDATE cms_workflow.users SET password_hash = '$2a$10$N.zmdr9k7uOsaLQJeuOISOXzDUz5vbMRoATWY4EABP/CL/8AUed0O' WHERE username = 'intake.analyst';
-- UPDATE cms_workflow.users SET password_hash = '$2a$10$N.zmdr9k7uOsaLQJeuOISOXzDUz5vbMRoATWY4EABP/CL/8AUed0O' WHERE username = 'admin';

-- The BCrypt hash '$2a$10$N.zmdr9k7uOsaLQJeuOISOXzDUz5vbMRoATWY4EABP/CL/8AUed0O' 
-- corresponds to the plain text password "demo123"

-- corresponds to the plain text password "demo123"
UPDATE cms_workflow.users SET password_hash = '$2a$10$EoM5TTJ9/7YlV1TD245gSei4AuZnbDtuEFewkOA7xl0j.h.W1rR4O';
--WHERE username = 'intake.analyst';