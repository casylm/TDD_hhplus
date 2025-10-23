package io.hhplus.tdd.point.domain;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }

    /**
     * 금액 유효성 검증
     */
    public UserPoint {
        if (point < 0) {
            throw new IllegalArgumentException("포인트 금액은 0 이상 입니다.");
        }
    }
}
