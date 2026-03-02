package com.moa2.api.auth.domain.repository;

import com.moa2.api.auth.domain.entity.AuthCode;
import org.springframework.data.repository.CrudRepository;

/**
 * 일회용 인증 코드(AuthCode) Redis Repository
 * - Spring Data Redis CrudRepository를 상속하여 자동 CRUD 구현
 * - Redis @RedisHash + @TimeToLive 설정에 의해 TTL이 지나면 자동 삭제
 */
public interface AuthCodeRepository extends CrudRepository<AuthCode, String> {
}
