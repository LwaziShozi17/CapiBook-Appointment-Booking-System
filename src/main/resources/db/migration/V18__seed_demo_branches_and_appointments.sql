-- =============================================================
-- V18: Demo seed — 10 SA branches, operating hours, customers,
--      and a realistic mix of booked / available appointments
-- =============================================================

-- ---------------------------------------------------------------
-- 1. BRANCHES  (one per province; Gauteng gets two)
-- ---------------------------------------------------------------
INSERT INTO branches (id, branch_code, name, address, city, province, postal_code,
                      latitude, longitude, phone_number, email, active,
                      max_concurrent_appointments, created_at, updated_at)
VALUES
    ('b0000000-0000-4000-8000-000000000001', 'CPB-JHB-001',
     'Sandton City', '163 5th St, Sandhurst', 'Johannesburg', 'Gauteng', '2196',
     -26.107567,  28.056702, '011 784 0100', 'sandton@capibook.co.za', TRUE, 3,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('b0000000-0000-4000-8000-000000000002', 'CPB-SOW-001',
     'Soweto Maponya Mall', 'Chris Hani Rd, Soweto', 'Soweto', 'Gauteng', '1809',
     -26.262800,  27.859200, '011 933 0200', 'soweto@capibook.co.za', TRUE, 2,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('b0000000-0000-4000-8000-000000000003', 'CPB-CPT-001',
     'Cape Town Waterfront', 'Shop 6251, V&A Waterfront', 'Cape Town', 'Western Cape', '8001',
     -33.903900,  18.419800, '021 418 0300', 'waterfront@capibook.co.za', TRUE, 3,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('b0000000-0000-4000-8000-000000000004', 'CPB-DBN-001',
     'Durban Workshop', '99 Commercial Rd, Workshop Mall', 'Durban', 'KwaZulu-Natal', '4001',
     -29.857500,  31.023500, '031 304 0400', 'durban@capibook.co.za', TRUE, 2,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('b0000000-0000-4000-8000-000000000005', 'CPB-GQB-001',
     'Gqeberha Greenacres', 'Greenacres Shopping Centre, Gqeberha', 'Gqeberha', 'Eastern Cape', '6045',
     -33.967800,  25.581400, '041 363 0500', 'gqeberha@capibook.co.za', TRUE, 2,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('b0000000-0000-4000-8000-000000000006', 'CPB-BFN-001',
     'Bloemfontein Mimosa Mall', 'Kellner St, Westdene', 'Bloemfontein', 'Free State', '9300',
     -29.130500,  26.198700, '051 444 0600', 'bloemfontein@capibook.co.za', TRUE, 2,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('b0000000-0000-4000-8000-000000000007', 'CPB-PLK-001',
     'Polokwane Mall of the North', 'Thabo Mbeki St, Polokwane', 'Polokwane', 'Limpopo', '0699',
     -23.896700,  29.447800, '015 291 0700', 'polokwane@capibook.co.za', TRUE, 2,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('b0000000-0000-4000-8000-000000000008', 'CPB-NLS-001',
     'Nelspruit Riverside Mall', 'Riverside Park, Mbombela', 'Mbombela', 'Mpumalanga', '1200',
     -25.460800,  30.976400, '013 752 0800', 'nelspruit@capibook.co.za', TRUE, 2,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('b0000000-0000-4000-8000-000000000009', 'CPB-RST-001',
     'Rustenburg Waterfall Mall', 'Fatima Bhayat St, Rustenburg', 'Rustenburg', 'North West', '0299',
     -25.667200,  27.243300, '014 592 0900', 'rustenburg@capibook.co.za', TRUE, 2,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('b0000000-0000-4000-8000-000000000010', 'CPB-KIM-001',
     'Kimberley Diamond Pavilion', 'Phakamile Mabija Rd, Kimberley', 'Kimberley', 'Northern Cape', '8300',
     -28.737400,  24.762200, '053 832 1000', 'kimberley@capibook.co.za', TRUE, 2,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


-- ---------------------------------------------------------------
-- 2. OPERATING HOURS  (Mon–Fri 08:00–17:00, Sat 08:00–13:00, closed Sun)
-- ---------------------------------------------------------------
INSERT INTO branch_operating_hours (id, branch_id, day_of_week, open_time, close_time, closed)
SELECT
    gen_random_uuid(),
    b.id,
    d.day_of_week,
    d.open_time::TIME,
    d.close_time::TIME,
    d.closed
FROM branches b
CROSS JOIN (VALUES
    ('MONDAY',    '08:00', '17:00', FALSE),
    ('TUESDAY',   '08:00', '17:00', FALSE),
    ('WEDNESDAY', '08:00', '17:00', FALSE),
    ('THURSDAY',  '08:00', '17:00', FALSE),
    ('FRIDAY',    '08:00', '17:00', FALSE),
    ('SATURDAY',  '08:00', '13:00', FALSE),
    ('SUNDAY',    NULL,    NULL,    TRUE)
) AS d(day_of_week, open_time, close_time, closed)
WHERE b.id::text LIKE 'b0000000-0000-4000-8000-%';


-- ---------------------------------------------------------------
-- 3. DEMO CUSTOMERS  (password_hash = bcrypt of 'Password@1')
-- ---------------------------------------------------------------
INSERT INTO users (id, email, password_hash, first_name, last_name, phone_number,
                   role, active, created_at, updated_at)
VALUES
    ('c0000000-0000-4000-8000-000000000001', 'sipho.ndlovu@gmail.com',
     '$2a$12$XLXME3FzgTSc7QFKP7sOZuXqjHBqhZcUvj5zxGz2DXlmknFRSlXRa',
     'Sipho', 'Ndlovu', '0821234001', 'CUSTOMER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('c0000000-0000-4000-8000-000000000002', 'naledi.mokoena@gmail.com',
     '$2a$12$XLXME3FzgTSc7QFKP7sOZuXqjHBqhZcUvj5zxGz2DXlmknFRSlXRa',
     'Naledi', 'Mokoena', '0831234002', 'CUSTOMER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('c0000000-0000-4000-8000-000000000003', 'thabo.dlamini@gmail.com',
     '$2a$12$XLXME3FzgTSc7QFKP7sOZuXqjHBqhZcUvj5zxGz2DXlmknFRSlXRa',
     'Thabo', 'Dlamini', '0841234003', 'CUSTOMER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('c0000000-0000-4000-8000-000000000004', 'ayesha.patel@gmail.com',
     '$2a$12$XLXME3FzgTSc7QFKP7sOZuXqjHBqhZcUvj5zxGz2DXlmknFRSlXRa',
     'Ayesha', 'Patel', '0851234004', 'CUSTOMER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('c0000000-0000-4000-8000-000000000005', 'pieter.vanzyl@gmail.com',
     '$2a$12$XLXME3FzgTSc7QFKP7sOZuXqjHBqhZcUvj5zxGz2DXlmknFRSlXRa',
     'Pieter', 'van Zyl', '0861234005', 'CUSTOMER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('c0000000-0000-4000-8000-000000000006', 'zanele.khumalo@gmail.com',
     '$2a$12$XLXME3FzgTSc7QFKP7sOZuXqjHBqhZcUvj5zxGz2DXlmknFRSlXRa',
     'Zanele', 'Khumalo', '0871234006', 'CUSTOMER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('c0000000-0000-4000-8000-000000000007', 'andre.botha@gmail.com',
     '$2a$12$XLXME3FzgTSc7QFKP7sOZuXqjHBqhZcUvj5zxGz2DXlmknFRSlXRa',
     'André', 'Botha', '0881234007', 'CUSTOMER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('c0000000-0000-4000-8000-000000000008', 'fatima.omar@gmail.com',
     '$2a$12$XLXME3FzgTSc7QFKP7sOZuXqjHBqhZcUvj5zxGz2DXlmknFRSlXRa',
     'Fatima', 'Omar', '0891234008', 'CUSTOMER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('c0000000-0000-4000-8000-000000000009', 'lebo.sithole@gmail.com',
     '$2a$12$XLXME3FzgTSc7QFKP7sOZuXqjHBqhZcUvj5zxGz2DXlmknFRSlXRa',
     'Lebo', 'Sithole', '0791234009', 'CUSTOMER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('c0000000-0000-4000-8000-000000000010', 'priya.naidoo@gmail.com',
     '$2a$12$XLXME3FzgTSc7QFKP7sOZuXqjHBqhZcUvj5zxGz2DXlmknFRSlXRa',
     'Priya', 'Naidoo', '0741234010', 'CUSTOMER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


-- ---------------------------------------------------------------
-- 4. APPOINTMENTS
--    Mix: COMPLETED (past), CONFIRMED (upcoming), CANCELLED
--    Dates are relative to seed date 2026-09-03
-- ---------------------------------------------------------------

-- ---- COMPLETED appointments (last week) ----
INSERT INTO appointments (id, customer_id, branch_id, service_id,
                          appointment_date, start_time, end_time,
                          status, reference_number, notes, version, created_at, updated_at)
VALUES
    ('a0000000-0000-4000-8000-000000000001',
     'c0000000-0000-4000-8000-000000000001',
     'b0000000-0000-4000-8000-000000000001',
     '00000000-0000-4000-8000-000000000001',
     '2026-08-28', '08:00', '08:15',
     'COMPLETED', 'CPB-20260828-0001', 'Card collected successfully.', 0,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('a0000000-0000-4000-8000-000000000002',
     'c0000000-0000-4000-8000-000000000002',
     'b0000000-0000-4000-8000-000000000003',
     '00000000-0000-4000-8000-000000000005',
     '2026-08-29', '09:30', '10:15',
     'COMPLETED', 'CPB-20260829-0002', NULL, 0,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('a0000000-0000-4000-8000-000000000003',
     'c0000000-0000-4000-8000-000000000003',
     'b0000000-0000-4000-8000-000000000004',
     '00000000-0000-4000-8000-000000000003',
     '2026-09-01', '11:00', '11:30',
     'COMPLETED', 'CPB-20260901-0003', 'Helped with account query.', 0,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('a0000000-0000-4000-8000-000000000004',
     'c0000000-0000-4000-8000-000000000004',
     'b0000000-0000-4000-8000-000000000002',
     '00000000-0000-4000-8000-000000000004',
     '2026-09-02', '14:00', '14:20',
     'COMPLETED', 'CPB-20260902-0004', NULL, 0,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- ---- CANCELLED appointments ----
    ('a0000000-0000-4000-8000-000000000005',
     'c0000000-0000-4000-8000-000000000005',
     'b0000000-0000-4000-8000-000000000005',
     '00000000-0000-4000-8000-000000000008',
     '2026-09-04', '10:00', '10:45',
     'CANCELLED', 'CPB-20260904-0005', 'Customer cancelled — rescheduling.', 0,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('a0000000-0000-4000-8000-000000000006',
     'c0000000-0000-4000-8000-000000000006',
     'b0000000-0000-4000-8000-000000000007',
     '00000000-0000-4000-8000-000000000002',
     '2026-09-05', '08:30', '08:50',
     'CANCELLED', 'CPB-20260905-0006', NULL, 0,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- ---- CONFIRMED upcoming appointments (today + next 7 days) ----

    -- Sandton City — today 2026-09-03
    ('a0000000-0000-4000-8000-000000000007',
     'c0000000-0000-4000-8000-000000000007',
     'b0000000-0000-4000-8000-000000000001',
     '00000000-0000-4000-8000-000000000005',
     '2026-09-03', '09:00', '09:45',
     'CONFIRMED', 'CPB-20260903-0007', NULL, 0,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('a0000000-0000-4000-8000-000000000008',
     'c0000000-0000-4000-8000-000000000008',
     'b0000000-0000-4000-8000-000000000001',
     '00000000-0000-4000-8000-000000000003',
     '2026-09-03', '10:30', '11:00',
     'CONFIRMED', 'CPB-20260903-0008', 'Will bring ID and bank statement.', 0,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- Cape Town Waterfront — 2026-09-04
    ('a0000000-0000-4000-8000-000000000009',
     'c0000000-0000-4000-8000-000000000009',
     'b0000000-0000-4000-8000-000000000003',
     '00000000-0000-4000-8000-000000000012',
     '2026-09-04', '09:00', '09:20',
     'CONFIRMED', 'CPB-20260904-0009', NULL, 0,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('a0000000-0000-4000-8000-000000000010',
     'c0000000-0000-4000-8000-000000000010',
     'b0000000-0000-4000-8000-000000000003',
     '00000000-0000-4000-8000-000000000009',
     '2026-09-04', '11:00', '11:45',
     'CONFIRMED', 'CPB-20260904-0010', 'Interested in funeral cover.', 0,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- Durban Workshop — 2026-09-05
    ('a0000000-0000-4000-8000-000000000011',
     'c0000000-0000-4000-8000-000000000001',
     'b0000000-0000-4000-8000-000000000004',
     '00000000-0000-4000-8000-000000000006',
     '2026-09-05', '14:00', '14:30',
     'CONFIRMED', 'CPB-20260905-0011', NULL, 0,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- Gqeberha — 2026-09-05
    ('a0000000-0000-4000-8000-000000000012',
     'c0000000-0000-4000-8000-000000000002',
     'b0000000-0000-4000-8000-000000000005',
     '00000000-0000-4000-8000-000000000007',
     '2026-09-05', '10:00', '10:30',
     'CONFIRMED', 'CPB-20260905-0012', NULL, 0,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- Bloemfontein — 2026-09-08
    ('a0000000-0000-4000-8000-000000000013',
     'c0000000-0000-4000-8000-000000000003',
     'b0000000-0000-4000-8000-000000000006',
     '00000000-0000-4000-8000-000000000001',
     '2026-09-08', '08:00', '08:15',
     'CONFIRMED', 'CPB-20260908-0013', NULL, 0,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- Polokwane — 2026-09-08
    ('a0000000-0000-4000-8000-000000000014',
     'c0000000-0000-4000-8000-000000000004',
     'b0000000-0000-4000-8000-000000000007',
     '00000000-0000-4000-8000-000000000011',
     '2026-09-08', '15:00', '15:30',
     'CONFIRMED', 'CPB-20260908-0014', 'Forex enquiry.', 0,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- Nelspruit — 2026-09-09
    ('a0000000-0000-4000-8000-000000000015',
     'c0000000-0000-4000-8000-000000000005',
     'b0000000-0000-4000-8000-000000000008',
     '00000000-0000-4000-8000-000000000008',
     '2026-09-09', '09:30', '10:15',
     'CONFIRMED', 'CPB-20260909-0015', NULL, 0,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- Rustenburg — 2026-09-09
    ('a0000000-0000-4000-8000-000000000016',
     'c0000000-0000-4000-8000-000000000006',
     'b0000000-0000-4000-8000-000000000009',
     '00000000-0000-4000-8000-000000000010',
     '2026-09-09', '11:00', '11:30',
     'CONFIRMED', 'CPB-20260909-0016', NULL, 0,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- Kimberley — 2026-09-10
    ('a0000000-0000-4000-8000-000000000017',
     'c0000000-0000-4000-8000-000000000007',
     'b0000000-0000-4000-8000-000000000010',
     '00000000-0000-4000-8000-000000000003',
     '2026-09-10', '13:30', '14:00',
     'CONFIRMED', 'CPB-20260910-0017', NULL, 0,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- Soweto — 2026-09-10
    ('a0000000-0000-4000-8000-000000000018',
     'c0000000-0000-4000-8000-000000000008',
     'b0000000-0000-4000-8000-000000000002',
     '00000000-0000-4000-8000-000000000004',
     '2026-09-10', '10:00', '10:20',
     'CONFIRMED', 'CPB-20260910-0018', 'Struggling with app login.', 0,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- ---- PENDING appointments (just booked, not yet confirmed) ----
    ('a0000000-0000-4000-8000-000000000019',
     'c0000000-0000-4000-8000-000000000009',
     'b0000000-0000-4000-8000-000000000001',
     '00000000-0000-4000-8000-000000000002',
     '2026-09-11', '08:30', '08:50',
     'PENDING', 'CPB-20260911-0019', NULL, 0,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('a0000000-0000-4000-8000-000000000020',
     'c0000000-0000-4000-8000-000000000010',
     'b0000000-0000-4000-8000-000000000003',
     '00000000-0000-4000-8000-000000000006',
     '2026-09-11', '14:00', '14:30',
     'PENDING', 'CPB-20260911-0020', NULL, 0,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);