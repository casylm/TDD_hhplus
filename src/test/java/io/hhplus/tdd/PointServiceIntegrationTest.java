package io.hhplus.tdd;

import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.TransactionType;
import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.repository.PointHistoryRepository;
import io.hhplus.tdd.point.repository.UserPointRepository;
import io.hhplus.tdd.point.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TddApplication.class)
@ExtendWith(SpringExtension.class)
public class PointServiceIntegrationTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    @Autowired
    private UserPointRepository userPointRepository;

    @Test
    @DisplayName("포인트 적립, 사용, 조회 통합 테스트")
    public void 포인트_적립_사용_조회_통합테스트() {
        // given
        long userId = 1L;
        long chargeAmount1 = 1000L;
        long chargeAmount2 = 500L;
        long useAmount = 300L;

        // when
        // 1. 초기 포인트 조회 (최초 사용자는 0 포인트)
        UserPoint initialPoint = pointService.getUserPoint(userId);
        // 2. 포인트 충전 (1000 포인트)
        UserPoint chargedPoint1 = pointService.chargePoint(userId, chargeAmount1);
        // 3. 포인트 추가 충전 (500 포인트)
        UserPoint chargedPoint2 = pointService.chargePoint(userId, chargeAmount2);
        // 4. 포인트 사용 (300 포인트)
        UserPoint usedPoint = pointService.usePoint(userId, useAmount);
        // 5. 최종 포인트 조회
        UserPoint finalPoint = pointService.getUserPoint(userId);
        // 6. 포인트 히스토리 조회
        List<PointHistory> histories = pointService.getPointHistories(userId);

        // then
        // 1. 초기 포인트는 0
        assertThat(initialPoint.point()).isEqualTo(0L);
        // 2. 첫 번째 충전 후 포인트는 1000
        assertThat(chargedPoint1.point()).isEqualTo(1000L);
        // 3. 두 번째 충전 후 포인트는 1500
        assertThat(chargedPoint2.point()).isEqualTo(1500L);
        // 4. 사용 후 포인트는 1200
        assertThat(usedPoint.point()).isEqualTo(1200L);
        // 5. 최종 포인트는 1200
        assertThat(finalPoint.point()).isEqualTo(1200L);
        assertThat(finalPoint.id()).isEqualTo(userId);

        // 6. 히스토리 3개 (충전 2번, 사용 1번)
        assertThat(histories).hasSize(3);

        // 7. 히스토리 내용 검증
        PointHistory history1 = histories.get(0);
        assertThat(history1.userId()).isEqualTo(userId);
        assertThat(history1.amount()).isEqualTo(chargeAmount1);
        assertThat(history1.type()).isEqualTo(TransactionType.CHARGE);

        PointHistory history2 = histories.get(1);
        assertThat(history2.userId()).isEqualTo(userId);
        assertThat(history2.amount()).isEqualTo(chargeAmount2);
        assertThat(history2.type()).isEqualTo(TransactionType.CHARGE);

        PointHistory history3 = histories.get(2);
        assertThat(history3.userId()).isEqualTo(userId);
        assertThat(history3.amount()).isEqualTo(useAmount);
        assertThat(history3.type()).isEqualTo(TransactionType.USE);
    }

    @Test
    @DisplayName("동일 유저에 대한 동시 충전 요청 - 순차 처리되어야 함")
    public void 동일_유저_동시_충전_테스트() throws InterruptedException {
        // given
        long userId = 100L;
        int threadCount = 10;
        long chargeAmountPerThread = 100L;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 10개 스레드가 동시에 100포인트씩 충전
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.chargePoint(userId, chargeAmountPerThread);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // 메인 스레드는 모든 스레드가 종료될 때까지 대기
        latch.await();
        executorService.shutdown();

        // then
        UserPoint finalPoint = pointService.getUserPoint(userId);
        List<PointHistory> histories = pointService.getPointHistories(userId);

        // 모든 요청이 성공해야 함
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(failCount.get()).isEqualTo(0);

        // 최종 포인트는 1000 (100 * 10)
        assertThat(finalPoint.point()).isEqualTo(1000L);

        // 히스토리는 10개
        assertThat(histories).hasSize(threadCount);
        assertThat(histories).allMatch(h -> h.type() == TransactionType.CHARGE);
    }

    @Test
    @DisplayName("동일 유저에 대한 동시 충전/사용 요청 - 순차 처리되어야 함")
    public void 동일_유저_동시_충전_사용_테스트() throws InterruptedException {
        // given
        long userId = 200L;
        int threadCount = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // 초기 포인트 충전 (10000 포인트)
        pointService.chargePoint(userId, 10000L);

        // when - 10개 스레드는 충전(100), 10개 스레드는 사용(50)
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    if (index % 2 == 0) {
                        pointService.chargePoint(userId, 100L);
                    } else {
                        pointService.usePoint(userId, 50L);
                    }
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 일부 사용 요청은 실패할 수 있음
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        UserPoint finalPoint = pointService.getUserPoint(userId);
        List<PointHistory> histories = pointService.getPointHistories(userId);

        // 최종 포인트 계산: 초기 10000 + (충전 100 * 10) - (사용 50 * 10) = 10500
        assertThat(finalPoint.point()).isEqualTo(10500L);

        // 히스토리는 초기 충전 1개 + 동시 요청 20개 = 21개
        assertThat(histories).hasSize(21);

        long chargeCount = histories.stream().filter(h -> h.type() == TransactionType.CHARGE).count();
        long useCount = histories.stream().filter(h -> h.type() == TransactionType.USE).count();

        assertThat(chargeCount).isEqualTo(11); // 초기 1 + 동시 충전 10
        assertThat(useCount).isEqualTo(10);
    }

    @Test
    @DisplayName("서로 다른 유저에 대한 동시 요청 - 독립적으로 처리되어야 함")
    public void 다른_유저_동시_요청_테스트() throws InterruptedException {
        // given
        int userCount = 5;
        int requestPerUser = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(userCount * requestPerUser);
        CountDownLatch latch = new CountDownLatch(userCount * requestPerUser);

        // when - 5명의 유저가 각각 10번씩 100포인트 충전
        for (int userId = 300; userId < 300 + userCount; userId++) {
            final long finalUserId = userId;
            for (int j = 0; j < requestPerUser; j++) {
                executorService.submit(() -> {
                    try {
                        pointService.chargePoint(finalUserId, 100L);
                    } catch (Exception e) {
                        // 예외 무시
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        latch.await();
        executorService.shutdown();

        // then - 각 유저의 포인트가 정확히 1000 (100 * 10)이어야 함
        for (int userId = 300; userId < 300 + userCount; userId++) {
            UserPoint userPoint = pointService.getUserPoint(userId);
            assertThat(userPoint.point()).isEqualTo(1000L);

            List<PointHistory> histories = pointService.getPointHistories(userId);
            assertThat(histories).hasSize(requestPerUser);
        }
    }

}
