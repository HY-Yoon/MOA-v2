package com.moa2.entity.payment;

// 결제 수단
public enum PaymentMethod {
    CARD,          // 카드
    EASY_PAY,      // 간편결제
    VIRTUAL_ACCOUNT, // 가상계좌 (이번 프로젝트 범위에선 제외될 수 있음)
    TOSS
}

