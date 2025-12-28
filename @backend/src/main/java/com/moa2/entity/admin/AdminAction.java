package com.moa2.entity.admin;

public enum AdminAction {
    CREATE,
    UPDATE,
    DELETE,
    FORCE_WITHDRAWAL, // 회원 강제 탈퇴
    FORCE_CANCEL,     // 예매 강제 취소
    RESTORE           // 회원 복구
}

