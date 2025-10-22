package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.domain.TransactionType;
import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.repository.PointHistoryRepository;
import io.hhplus.tdd.point.repository.UserPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;

    // 1. 포인트 조회
    public UserPoint getUserPoint(long id) {
        return userPointRepository.selectById(id);
    }

    // 2. 포인트 충전
    public UserPoint chargePoint(long id, long amount) {
        // 현재 포인트 조회
        UserPoint userPoint = userPointRepository.selectById(id);
        long newAmount = userPoint.point() + amount;

        // 포인트 충전
        UserPoint updatedPoint = userPointRepository.insertOrUpdate(id,newAmount);

        // 충전 내역 저장
        pointHistoryRepository.insert(id, amount, TransactionType.CHARGE, updatedPoint.updateMillis());

        return updatedPoint;
    }

    // 3. 포인트 사용
    public UserPoint usePoint(long id, long amount) {
        // 현재 포인트 조회
        UserPoint userPoint = userPointRepository.selectById(id);
        long newAmount = userPoint.point() - amount;

        // 포인트 사용
        UserPoint updatedPoint = userPointRepository.insertOrUpdate(id,newAmount);

        // 충전 내역 저장
        pointHistoryRepository.insert(id, amount, TransactionType.USE, updatedPoint.updateMillis());

        return updatedPoint;
    }

}
