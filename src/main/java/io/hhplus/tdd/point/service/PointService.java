package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.TransactionType;
import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.repository.PointHistoryRepository;
import io.hhplus.tdd.point.repository.UserPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;

    // 사용자별 락을 관리하는 ConcurrentHashMap
    private final ConcurrentHashMap<Long, Lock> userLocks = new ConcurrentHashMap<>();

    /**
     * 락이 없으면 새로 생성, 있으면 기존 락 반환.
     */
    private Lock getUserLock(long userId) {
        return userLocks.computeIfAbsent(userId, key -> new ReentrantLock());
    }

    // 1. 포인트 조회
    public UserPoint getUserPoint(long id) {
        UserPoint userPoint = userPointRepository.selectById(id);

        if (userPoint == null) {
            return UserPoint.empty(id);
        }
        return userPoint;
    }

    // 2. 포인트 충전
    public UserPoint chargePoint(long id, long amount) {
        // 입력 검증
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }

        Lock lock = getUserLock(id);
        lock.lock();

        try {
            // 현재 포인트 조회
            UserPoint userPoint = userPointRepository.selectById(id);
            long newAmount = userPoint.point() + amount;

            final long MAX_POINT = 1_000_000L; // 최대 100만 포인트

            if (newAmount > MAX_POINT) {
                throw new IllegalArgumentException("최대 포인트 한도(" + MAX_POINT + ")를 초과할 수 없습니다.");
            }

            // 포인트 충전
            UserPoint updatedPoint = userPointRepository.insertOrUpdate(id, newAmount);

            // 충전 내역 저장
            pointHistoryRepository.insert(id, amount, TransactionType.CHARGE, updatedPoint.updateMillis());

            return updatedPoint;
        } finally {
            lock.unlock();
        }
    }

    // 3. 포인트 사용
    public UserPoint usePoint(long id, long amount) {
        // 입력 검증
        if (amount <= 0) {
            throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다.");
        }

        Lock lock = getUserLock(id);
        lock.lock();
        try {
            // 현재 포인트 조회
            UserPoint userPoint = userPointRepository.selectById(id);
            long newAmount = userPoint.point() - amount;

            if (newAmount < 0) {
                throw new IllegalArgumentException("포인트가 부족하여 차감할 수 없습니다.");
            }

            // 포인트 사용
            UserPoint updatedPoint = userPointRepository.insertOrUpdate(id, newAmount);

            // 사용 내역 저장
            pointHistoryRepository.insert(id, amount, TransactionType.USE, updatedPoint.updateMillis());

            return updatedPoint;
        } finally {
            lock.unlock();
        }
    }

    // 4. 포인트 내역 조회
    public List<PointHistory> getPointHistories(long id) {
        return pointHistoryRepository.selectAllByUserId(id);
    }

}
