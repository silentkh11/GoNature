-- ============================================================
--  GoNature – National Park Visitor Management System
--  Group 7  |  Assignment 3
--  Database schema and seed data for MySQL 8.x
--  Run this script once to create and populate the database.
-- ============================================================

-- Create the database (skip if it already exists)
CREATE DATABASE IF NOT EXISTS gonature_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE gonature_db;

-- ============================================================
-- TABLE: park
-- Stores configuration and live state for each national park.
-- ============================================================
CREATE TABLE IF NOT EXISTS park (
    park_id              INT PRIMARY KEY AUTO_INCREMENT,
    name                 VARCHAR(100) NOT NULL,
    max_capacity         INT          NOT NULL DEFAULT 500,
    casual_gap           INT          NOT NULL DEFAULT 50,
    estimated_stay_time  INT          NOT NULL DEFAULT 3,
    current_visitors     INT          NOT NULL DEFAULT 0,
    active_discount      DECIMAL(5,2) NOT NULL DEFAULT 0.00
);

-- ============================================================
-- TABLE: visitor
-- Stores every person who has ever booked or entered a park.
-- visitor_type: 'Regular' | 'Subscriber' | 'Guide'
-- ============================================================
CREATE TABLE IF NOT EXISTS visitor (
    visitor_id   VARCHAR(20)  NOT NULL PRIMARY KEY,
    first_name   VARCHAR(50)  NOT NULL DEFAULT 'Guest',
    last_name    VARCHAR(50)  NOT NULL DEFAULT 'Visitor',
    email        VARCHAR(255) NOT NULL DEFAULT 'Not Provided',
    phone        VARCHAR(20)  NOT NULL DEFAULT 'Not Provided',
    visitor_type VARCHAR(20)  NOT NULL DEFAULT 'Regular'
);

-- ============================================================
-- TABLE: employee
-- Stores staff accounts.
-- role: 'Park Manager' | 'Dept Manager' | 'Gate Worker' | 'Service Rep'
-- park_id is NULL for Department Managers (they oversee all parks).
-- ============================================================
CREATE TABLE IF NOT EXISTS employee (
    employee_id  INT PRIMARY KEY AUTO_INCREMENT,
    username     VARCHAR(50)  NOT NULL UNIQUE,
    password     VARCHAR(100) NOT NULL,
    first_name   VARCHAR(50)  NOT NULL,
    last_name    VARCHAR(50)  NOT NULL,
    email        VARCHAR(255) NOT NULL,
    role         VARCHAR(30)  NOT NULL,
    park_id      INT          NULL
);

-- ============================================================
-- TABLE: subscriber
-- Members and certified tour guides.
-- is_guide = 1 means the visitor is a certified tour guide.
-- ============================================================
CREATE TABLE IF NOT EXISTS subscriber (
    subscriber_id INT PRIMARY KEY AUTO_INCREMENT,
    visitor_id    VARCHAR(20)  NOT NULL,
    family_size   INT          NOT NULL DEFAULT 1,
    credit_card   VARCHAR(30)  NULL,
    is_guide      TINYINT(1)   NOT NULL DEFAULT 0,
    FOREIGN KEY (visitor_id) REFERENCES visitor(visitor_id)
);

-- ============================================================
-- TABLE: visit_order
-- Every booking (pre-planned or walk-in) and its full lifecycle.
-- status flow:
--   Booked → Pending Confirm → Confirmed → In Park → Completed
--   Booked → Waitlisted → Waitlist Pending → Confirmed → ...
--   Any terminal state: Cancelled | Expired
-- order_type: 'Personal' | 'Family' | 'Group'
-- ============================================================
CREATE TABLE IF NOT EXISTS visit_order (
    order_id           INT          PRIMARY KEY AUTO_INCREMENT,
    park_id            INT          NOT NULL,
    visitor_id         VARCHAR(20)  NOT NULL,
    visit_date         DATE         NOT NULL,
    visit_time         TIME         NOT NULL,
    visitor_count      INT          NOT NULL DEFAULT 1,
    order_type         VARCHAR(20)  NOT NULL DEFAULT 'Personal',
    status             VARCHAR(30)  NOT NULL DEFAULT 'Booked',
    price              DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    email              VARCHAR(255) NULL,
    phone              VARCHAR(20)  NULL,
    entry_timestamp    TIMESTAMP    NULL DEFAULT NULL,
    exit_timestamp     TIMESTAMP    NULL DEFAULT NULL,
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notification_time  TIMESTAMP    NULL DEFAULT NULL,
    FOREIGN KEY (park_id)    REFERENCES park(park_id),
    FOREIGN KEY (visitor_id) REFERENCES visitor(visitor_id)
);

-- ============================================================
-- TABLE: parameter_request
-- Park parameter change requests submitted by Park Managers
-- and pending approval/denial by the Department Manager.
-- status: 'Pending' | 'Approved' | 'Denied'
-- ============================================================
CREATE TABLE IF NOT EXISTS parameter_request (
    request_id               INT PRIMARY KEY AUTO_INCREMENT,
    park_id                  INT         NOT NULL,
    new_max_capacity         INT         NOT NULL,
    new_casual_gap           INT         NOT NULL,
    new_estimated_stay_time  INT         NOT NULL,
    status                   VARCHAR(20) NOT NULL DEFAULT 'Pending',
    FOREIGN KEY (park_id) REFERENCES park(park_id)
);

-- ============================================================
-- TABLE: park_promotion
-- Promotional discount requests submitted by Park Managers
-- and pending approval/denial/cancellation by the Department Manager.
-- status: 'Pending' | 'Approved' | 'Denied' | 'Cancelled'
-- ============================================================
CREATE TABLE IF NOT EXISTS park_promotion (
    promotion_id     INT PRIMARY KEY AUTO_INCREMENT,
    park_id          INT          NOT NULL,
    park_name        VARCHAR(100) NOT NULL,
    discount_percent DECIMAL(5,2) NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'Pending',
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (park_id) REFERENCES park(park_id)
);

-- ============================================================
-- SEED DATA: Parks (8 national parks)
-- ============================================================
INSERT IGNORE INTO park (park_id, name, max_capacity, casual_gap, estimated_stay_time, current_visitors, active_discount)
VALUES
    (1, 'Banias Nature Reserve',        500, 50, 3, 0, 0.00),
    (2, 'Carmel National Park',         700, 70, 4, 0, 0.00),
    (3, 'Galilee Mountain Park',        400, 40, 3, 0, 0.00),
    (4, 'Negev Desert Reserve',         300, 30, 5, 0, 0.00),
    (5, 'Masada National Park',         800, 80, 3, 0, 0.00),
    (6, 'Caesarea National Park',       600, 60, 2, 0, 0.00),
    (7, 'Timna Valley Park',            400, 40, 3, 0, 0.00),
    (8, 'Nahal Ayun Nature Reserve',    200, 20, 2, 0, 0.00);

-- ============================================================
-- SEED DATA: Employees
-- Passwords stored as plain text (course demo only — see README).
--
-- Park Manager accounts: pm1 – pm8  (password: pm1pass – pm8pass)
-- Department Manager   : deptmgr    (password: deptpass)
-- Gate Workers         : gate1, gate2 (password: gate1pass, gate2pass)
-- Service Reps         : service1, service2 (password: s1pass, s2pass)
-- ============================================================
INSERT IGNORE INTO employee (employee_id, username, password, first_name, last_name, email, role, park_id)
VALUES
    -- Department Manager (no specific park)
    (1,  'deptmgr',  'deptpass',  'Rachel',  'Cohen',    'rachel.cohen@gonature.gov.il',   'Dept Manager',  NULL),

    -- Park Managers (one per park)
    (2,  'pm1',      'pm1pass',   'Avi',     'Levi',     'avi.levi@gonature.gov.il',       'Park Manager',  1),
    (3,  'pm2',      'pm2pass',   'Dana',    'Katz',     'dana.katz@gonature.gov.il',      'Park Manager',  2),
    (4,  'pm3',      'pm3pass',   'Eli',     'Ben-David', 'eli.bendavid@gonature.gov.il',  'Park Manager',  3),
    (5,  'pm4',      'pm4pass',   'Miriam',  'Shapiro',  'miriam.shapiro@gonature.gov.il', 'Park Manager',  4),
    (6,  'pm5',      'pm5pass',   'Yossi',   'Mizrahi',  'yossi.mizrahi@gonature.gov.il',  'Park Manager',  5),
    (7,  'pm6',      'pm6pass',   'Shira',   'Peretz',   'shira.peretz@gonature.gov.il',   'Park Manager',  6),
    (8,  'pm7',      'pm7pass',   'Moshe',   'Golan',    'moshe.golan@gonature.gov.il',    'Park Manager',  7),
    (9,  'pm8',      'pm8pass',   'Naomi',   'Stern',    'naomi.stern@gonature.gov.il',    'Park Manager',  8),

    -- Gate Workers
    (10, 'gate1',    'gate1pass', 'Tamar',   'Hadad',    'tamar.hadad@gonature.gov.il',    'Gate Worker',   1),
    (11, 'gate2',    'gate2pass', 'Ori',     'Avraham',  'ori.avraham@gonature.gov.il',    'Gate Worker',   2),

    -- Service Representatives
    (12, 'service1', 's1pass',    'Gal',     'Nir',      'gal.nir@gonature.gov.il',        'Service Rep',   1),
    (13, 'service2', 's2pass',    'Maya',    'Ron',      'maya.ron@gonature.gov.il',       'Service Rep',   2);

-- ============================================================
-- SEED DATA: Visitors (sample)
-- ============================================================
INSERT IGNORE INTO visitor (visitor_id, first_name, last_name, email, phone, visitor_type)
VALUES
    ('123456789', 'Lior',   'Ben-Ami',  'lior@example.com',    '0501234567', 'Regular'),
    ('987654321', 'Noa',    'Katz',     'noa@example.com',     '0521234567', 'Subscriber'),
    ('111222333', 'Daniel', 'Levy',     'daniel@example.com',  '0541234567', 'Guide');

-- ============================================================
-- SEED DATA: Subscribers (sample)
-- ============================================================
INSERT IGNORE INTO subscriber (visitor_id, family_size, credit_card, is_guide)
VALUES
    ('987654321', 3, '4111111111111111', 0),
    ('111222333', 1, '5500005555555559', 1);

-- ============================================================
-- SEED DATA: Sample visit orders
-- ============================================================
INSERT IGNORE INTO visit_order (order_id, park_id, visitor_id, visit_date, visit_time, visitor_count, order_type, status, price)
VALUES
    (1, 1, '123456789', CURDATE(), '10:00:00', 2, 'Personal',  'Booked',    170.00),
    (2, 2, '987654321', CURDATE(), '11:00:00', 4, 'Family',    'Confirmed', 306.00),
    (3, 5, '111222333', CURDATE(), '09:00:00', 8, 'Group',     'Completed', 504.00);
