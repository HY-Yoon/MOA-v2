-- 제약 조건 수정 (실행 순서 문제로 인해 주석 처리, 필요시 DB에서 직접 실행)
-- ALTER TABLE shows DROP CONSTRAINT IF EXISTS shows_genre_check;
-- ALTER TABLE shows ADD CONSTRAINT shows_genre_check CHECK (genre IN ('MUSICAL', 'CONCERT', 'THEATER', 'CLASSIC', 'DANCE'));
-- ALTER TABLE shows DROP CONSTRAINT IF EXISTS shows_sale_status_check;
-- ALTER TABLE shows ADD CONSTRAINT shows_sale_status_check CHECK (sale_status IN ('ALLOWED', 'SUSPENDED'));

-- Venue 샘플 데이터 (이미 있을 수 있음)
INSERT INTO venues (id, name, hall_name, region, address, total_seats, latitude, longitude, seat_layout_image_url, created_at, updated_at)
VALUES 
(1, '예술의전당', '오페라극장', 'SEOUL', '서울특별시 서초구 남부순환로 2406', 2000, 37.4785, 127.0128, '/uploads/seat-layouts/opera-theater.png', NOW(), NOW()),
(2, '세종문화회관', '대극장', 'SEOUL', '서울특별시 종로구 세종대로 175', 3000, 37.5729, 126.9769, '/uploads/seat-layouts/sejong-main.png', NOW(), NOW()),
(3, '블루스퀘어', '신한카드홀', 'SEOUL', '서울특별시 용산구 이태원로 294', 1500, 37.5431, 126.9947, '/uploads/seat-layouts/blue-square.png', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Show 샘플 데이터
-- 날짜 형식: YYYY-MM-DD (LocalDate), YYYY-MM-DD HH:MI:SS (LocalDateTime)
INSERT INTO shows (
    id, venue_id, title, genre, running_time, poster_url, "cast", 
    detail_image_urls, start_date, end_date, status, sale_status,
    sale_start_date, sale_end_date, view_count, created_at, updated_at
)
VALUES 
-- 공연 1: 레미제라블 (판매중)
(1, 1, '레미제라블', 'MUSICAL', 150, '/uploads/posters/les-miserables.jpg', 
 '김철수, 이영희, 박민수', 
 ARRAY['/uploads/details/les-mis-1.jpg', '/uploads/details/les-mis-2.jpg'],
 '2024-02-01', '2024-04-30', 'ON_SALE', 'ALLOWED',
 '2024-01-15 10:00:00', '2024-04-30 23:59:59', 0,
 '2024-01-01 09:00:00', '2024-01-01 09:00:00'),

-- 공연 2: 오페라의 유령 (판매중)
(2, 1, '오페라의 유령', 'MUSICAL', 140, '/uploads/posters/phantom.jpg',
 '정민호, 최수진, 강태영',
 ARRAY['/uploads/details/phantom-1.jpg'],
 '2024-03-01', '2024-05-31', 'ON_SALE', 'ALLOWED',
 '2024-02-01 10:00:00', '2024-05-31 23:59:59', 0,
 '2024-01-15 10:00:00', '2024-01-15 10:00:00'),

-- 공연 3: 위키드 (대기중)
(3, 2, '위키드', 'MUSICAL', 165, '/uploads/posters/wicked.jpg',
 '송하나, 윤지원, 이대호',
 ARRAY['/uploads/details/wicked-1.jpg', '/uploads/details/wicked-2.jpg'],
 '2024-06-01', '2024-08-31', 'WAITING', 'ALLOWED',
 '2024-05-01 10:00:00', '2024-08-31 23:59:59', 0,
 '2024-02-01 11:00:00', '2024-02-01 11:00:00'),

-- 공연 4: 햄릿 (판매중) - THEATER 대신 MUSICAL 사용 (DB 제약 조건 이슈로 인해)
(4, 3, '햄릿', 'MUSICAL', 180, '/uploads/posters/hamlet.jpg',
 '조성민, 한소희',
 ARRAY['/uploads/details/hamlet-1.jpg'],
 '2024-02-15', '2024-03-15', 'ON_SALE', 'ALLOWED',
 '2024-01-20 10:00:00', '2024-03-15 23:59:59', 0,
 '2024-01-10 14:00:00', '2024-01-10 14:00:00'),

-- 공연 5: 베토벤 교향곡 (종료) - SUSPENDED 대신 ALLOWED 사용 (DB 제약 조건 이슈로 인해)
(5, 2, '베토벤 교향곡 9번', 'CLASSIC', 90, '/uploads/posters/beethoven.jpg',
 '서울심포니오케스트라',
 ARRAY['/uploads/details/beethoven-1.jpg'],
 '2023-12-01', '2023-12-31', 'ENDED', 'ALLOWED',
 '2023-11-01 10:00:00', '2023-12-31 23:59:59', 1250,
 '2023-10-15 09:00:00', '2023-12-31 23:59:59')
ON CONFLICT (id) DO NOTHING;

-- ShowSchedule 샘플 데이터
-- 날짜 형식: YYYY-MM-DD (showDate), HH:MI:SS (showTime), YYYY-MM-DD HH:MI:SS (ticketOpenTime)
INSERT INTO show_schedules (id, show_id, show_date, show_time, ticket_open_time, status)
VALUES 
-- 레미제라블 스케줄 (2월)
(1, 1, '2024-02-01', '19:00:00', '2024-01-15 10:00:00', 'AVAILABLE'),
(2, 1, '2024-02-02', '19:00:00', '2024-01-15 10:00:00', 'AVAILABLE'),
(3, 1, '2024-02-03', '14:00:00', '2024-01-15 10:00:00', 'AVAILABLE'),
(4, 1, '2024-02-10', '19:00:00', '2024-01-15 10:00:00', 'AVAILABLE'),
(5, 1, '2024-02-17', '19:00:00', '2024-01-15 10:00:00', 'AVAILABLE'),

-- 오페라의 유령 스케줄 (3월)
(6, 2, '2024-03-01', '19:30:00', '2024-02-01 10:00:00', 'AVAILABLE'),
(7, 2, '2024-03-02', '19:30:00', '2024-02-01 10:00:00', 'AVAILABLE'),
(8, 2, '2024-03-09', '14:30:00', '2024-02-01 10:00:00', 'AVAILABLE'),
(9, 2, '2024-03-16', '19:30:00', '2024-02-01 10:00:00', 'AVAILABLE'),

-- 위키드 스케줄 (6월)
(10, 3, '2024-06-01', '20:00:00', '2024-05-01 10:00:00', 'AVAILABLE'),
(11, 3, '2024-06-02', '20:00:00', '2024-05-01 10:00:00', 'AVAILABLE'),
(12, 3, '2024-06-08', '15:00:00', '2024-05-01 10:00:00', 'AVAILABLE'),

-- 햄릿 스케줄 (2-3월)
(13, 4, '2024-02-15', '19:00:00', '2024-01-20 10:00:00', 'AVAILABLE'),
(14, 4, '2024-02-16', '19:00:00', '2024-01-20 10:00:00', 'AVAILABLE'),
(15, 4, '2024-02-22', '19:00:00', '2024-01-20 10:00:00', 'AVAILABLE'),
(16, 4, '2024-03-01', '19:00:00', '2024-01-20 10:00:00', 'AVAILABLE'),

-- 베토벤 교향곡 스케줄 (12월 - 종료된 공연)
(17, 5, '2023-12-10', '19:30:00', '2023-11-01 10:00:00', 'COMPLETED'),
(18, 5, '2023-12-17', '19:30:00', '2023-11-01 10:00:00', 'COMPLETED'),
(19, 5, '2023-12-24', '19:30:00', '2023-11-01 10:00:00', 'COMPLETED')
ON CONFLICT (id) DO NOTHING;

-- SeatLayout 샘플 데이터 (Venue 1번용)
INSERT INTO seat_layout (id, venue_id, name, created_at, updated_at)
VALUES 
(1, 1, '오페라극장 뮤지컬 표준 배치도', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- VenueSeatSection 샘플 데이터 (기본 가격 정보)
INSERT INTO venue_seat_sections (id, seat_layout_id, section_name, default_price, created_at, updated_at)
VALUES 
(1, 1, 'VIP석', 150000, NOW(), NOW()),
(2, 1, 'R석', 120000, NOW(), NOW()),
(3, 1, 'S석', 90000, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 기본 가격 업데이트 (이미 INSERT된 경우)
UPDATE venue_seat_sections SET default_price = 150000 WHERE id = 1;
UPDATE venue_seat_sections SET default_price = 120000 WHERE id = 2;
UPDATE venue_seat_sections SET default_price = 90000 WHERE id = 3;

-- 시퀀스 재설정 (테스트 데이터 INSERT 후 시퀀스를 현재 최대 ID 값으로 동기화)
-- 이렇게 하면 다음 INSERT 시 중복 키 오류가 발생하지 않습니다
SELECT setval('shows_id_seq', COALESCE((SELECT MAX(id) FROM shows), 1), true);
SELECT setval('show_schedules_id_seq', COALESCE((SELECT MAX(id) FROM show_schedules), 1), true);
SELECT setval('venues_id_seq', COALESCE((SELECT MAX(id) FROM venues), 1), true);
SELECT setval('seat_layout_id_seq', COALESCE((SELECT MAX(id) FROM seat_layout), 1), true);
SELECT setval('venue_seat_sections_id_seq', COALESCE((SELECT MAX(id) FROM venue_seat_sections), 1), true);
