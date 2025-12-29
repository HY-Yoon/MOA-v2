-- 테스트용 더미 데이터
-- Venue (장소) 데이터
INSERT INTO venues (id, name, hall_name, region, address, total_seats, created_at, updated_at)
VALUES (1, '예술의전당', '오페라극장', 'SEOUL', '서울특별시 서초구 남부순환로 2406', 2000, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- SeatLayout (좌석 배치도) 데이터
INSERT INTO seat_layout (id, venue_id, name)
VALUES (1, 1, '오페라극장 뮤지컬 표준 배치도')
ON CONFLICT (id) DO NOTHING;

-- VenueSeatSection (구역 정보) 데이터
-- default_price는 Hibernate가 컬럼을 생성한 후 UPDATE로 설정
INSERT INTO venue_seat_sections (id, venue_id, name, display_order, created_at, updated_at)
VALUES 
    (1, 1, 'VIP석', 1, NOW(), NOW()),
    (2, 1, 'R석', 2, NOW(), NOW()),
    (3, 1, 'S석', 3, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    display_order = EXCLUDED.display_order,
    updated_at = NOW();

-- default_price 업데이트 (Hibernate가 컬럼을 생성한 후 실행)
UPDATE venue_seat_sections SET default_price = 150000 WHERE id = 1 AND default_price IS NULL;
UPDATE venue_seat_sections SET default_price = 120000 WHERE id = 2 AND default_price IS NULL;
UPDATE venue_seat_sections SET default_price = 90000 WHERE id = 3 AND default_price IS NULL;

