-- Genre check constraint 수정 (THEATER 포함하도록)
-- 이 파일은 테이블 생성 후, 데이터 삽입 전에 실행됩니다.
ALTER TABLE shows DROP CONSTRAINT IF EXISTS shows_genre_check;
ALTER TABLE shows ADD CONSTRAINT shows_genre_check CHECK (genre IN ('MUSICAL', 'CONCERT', 'THEATER', 'CLASSIC', 'DANCE'));

-- SaleStatus check constraint 수정 (SUSPENDED 포함하도록)
ALTER TABLE shows DROP CONSTRAINT IF EXISTS shows_sale_status_check;
ALTER TABLE shows ADD CONSTRAINT shows_sale_status_check CHECK (sale_status IN ('ALLOWED', 'SUSPENDED'));

-- ShowStatus check constraint 추가 (SUSPENDED 포함하도록)
ALTER TABLE shows DROP CONSTRAINT IF EXISTS shows_status_check;
ALTER TABLE shows ADD CONSTRAINT shows_status_check CHECK (status IN ('WAITING', 'ON_SALE', 'SOLD_OUT', 'ENDED', 'SUSPENDED'));

