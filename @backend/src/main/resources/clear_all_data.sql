-- 전체 데이터 삭제 (테스트용)
-- 주의: 이 스크립트는 모든 데이터를 삭제합니다. 프로덕션 환경에서 사용하지 마세요.

-- 외래키 제약조건 때문에 삭제 순서가 중요합니다.
-- 자식 테이블부터 삭제해야 합니다.

-- 1. 예매 관련 테이블 (가장 자식)
TRUNCATE TABLE reservation_seats CASCADE;
TRUNCATE TABLE reservations CASCADE;
TRUNCATE TABLE payments CASCADE;

-- 2. 공연 관련 테이블
TRUNCATE TABLE show_seat_grades CASCADE;
TRUNCATE TABLE show_cast_schedules CASCADE;
TRUNCATE TABLE show_schedules CASCADE;
TRUNCATE TABLE show_casts CASCADE;
TRUNCATE TABLE shows CASCADE;

-- 3. 좌석 관련 테이블
TRUNCATE TABLE seats CASCADE;
TRUNCATE TABLE venue_seat_sections CASCADE;
TRUNCATE TABLE seat_layout CASCADE;
TRUNCATE TABLE seat_maps CASCADE;

-- 4. 장소 관련 테이블
TRUNCATE TABLE venues CASCADE;

-- 5. 사용자 관련 테이블
TRUNCATE TABLE admin_logs CASCADE;
TRUNCATE TABLE users CASCADE;

-- 6. 시퀀스 재설정 (모든 테이블의 시퀀스를 1로 초기화)
SELECT setval('users_id_seq', 1, false);
SELECT setval('venues_id_seq', 1, false);
SELECT setval('shows_id_seq', 1, false);
SELECT setval('show_schedules_id_seq', 1, false);
SELECT setval('show_seat_grades_id_seq', 1, false);
SELECT setval('venue_seat_sections_id_seq', 1, false);
SELECT setval('seat_layout_id_seq', 1, false);
SELECT setval('seat_maps_id_seq', 1, false);
SELECT setval('seats_id_seq', 1, false);
SELECT setval('reservations_id_seq', 1, false);
SELECT setval('reservation_seats_id_seq', 1, false);
SELECT setval('payments_id_seq', 1, false);
SELECT setval('admin_logs_id_seq', 1, false);

-- 완료 메시지
SELECT 'All data cleared successfully!' AS message;

