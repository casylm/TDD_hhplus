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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
@DisplayName("PointService 테스트")
public class PointServiceTest {

    @InjectMocks
    private PointService pointService;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @Mock
    private UserPointRepository userPointRepository;

    @Test
    void 사용자의_포인트를_조회한다() {
        // given
        long givenPoint = 1000L;
        long userId = 1L;
        UserPoint userPoint = new UserPoint(userId,givenPoint,System.currentTimeMillis());

        // when
        when(userPointRepository.selectById(userId)).thenReturn(userPoint);

        // then
        UserPoint result = pointService.getUserPoint(userId);

        assertThat(result.point()).isEqualTo(givenPoint);
    }

    @Test
    void 포인트를_충전한다() {
        // given
        long currentPoint = 2500L;
        long chargePoint = 1000L;
        long userId = 2L;
        UserPoint userPoint = new UserPoint(userId,currentPoint,System.currentTimeMillis());
        UserPoint updatedPoint = new UserPoint(userId, currentPoint + chargePoint, System.currentTimeMillis());

        // when
        when(userPointRepository.selectById(userId)).thenReturn(userPoint);
        when(userPointRepository.insertOrUpdate(userId,currentPoint+chargePoint))
                .thenReturn(updatedPoint);

        // then
        UserPoint result = pointService.chargePoint(userId, chargePoint);

        assertThat(result.point()).isEqualTo(currentPoint+chargePoint);
    }

    @Test
    void 포인트를_사용한다() {
        // given
        long userId = 3L;
        long currentAmount = 50000L;
        long useAmount = 25000L;
        UserPoint beforeUser = new UserPoint(userId,currentAmount,System.currentTimeMillis());
        UserPoint AfterUser = new UserPoint(userId, currentAmount - useAmount, System.currentTimeMillis());

        // when
        when(userPointRepository.selectById(userId)).thenReturn(beforeUser);
        when(userPointRepository.insertOrUpdate(userId,currentAmount-useAmount))
                .thenReturn(AfterUser);
        // then
        UserPoint result = pointService.usePoint(userId, useAmount);

        assertThat(result.point()).isEqualTo(currentAmount-useAmount);
    }

    @Test
    @DisplayName("포인트 적립 후 내역을 조회한다")
    void 포인트_적립_후_내역을_조회한다() {
        // given
        long userId = 4L;
        long currentPoint = 1000L;
        long chargeAmount = 5000L;
        long updateMillis = System.currentTimeMillis();

        UserPoint currentUserPoint = new UserPoint(userId, currentPoint, updateMillis);
        UserPoint updatedUserPoint = new UserPoint(userId, currentPoint + chargeAmount, updateMillis);
        PointHistory pointHistory = new PointHistory(1L, userId, chargeAmount, TransactionType.CHARGE, updateMillis);

        // when
        when(userPointRepository.selectById(userId)).thenReturn(currentUserPoint);
        when(userPointRepository.insertOrUpdate(userId, currentPoint + chargeAmount))
                .thenReturn(updatedUserPoint);
        when(pointHistoryRepository.insert(eq(userId), eq(chargeAmount), eq(TransactionType.CHARGE), anyLong()))
                .thenReturn(pointHistory);
        when(pointHistoryRepository.selectAllByUserId(userId))
                .thenReturn(List.of(pointHistory));

        // 포인트 충전
        pointService.chargePoint(userId, chargeAmount);

        // then
        List<PointHistory> histories = pointService.getPointHistories(userId);

        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).userId()).isEqualTo(userId);
        assertThat(histories.get(0).amount()).isEqualTo(chargeAmount);
        assertThat(histories.get(0).type()).isEqualTo(TransactionType.CHARGE);

        // 충전 내역 저장이 호출되었는지 검증
        verify(pointHistoryRepository, times(1))
                .insert(eq(userId), eq(chargeAmount), eq(TransactionType.CHARGE), anyLong());
    }

    // Step02
    @Test
    @DisplayName("포인트 유효성 검사")
    void 사용자의_포인트가_0원_이하이면_예외발생() {
        // given
        long userId = 5L;

        // when & then
        assertThatThrownBy(() -> new UserPoint(5L,-500,System.currentTimeMillis()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("포인트 금액은 0 이상 입니다.");
    }

    @Test
    @DisplayName("포인트 충전 초과 검증")
    void 포인트_충전_초과_검증() {
        // given
        long userId = 6L;
        long currentPoint = 9_000_000L;
        UserPoint userPoint = new UserPoint(userId,currentPoint,System.currentTimeMillis());

        // when
        when(userPointRepository.selectById(userId)).thenReturn(userPoint);

        // then
        assertThatThrownBy(() -> pointService.chargePoint(userId,2_000_000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최대 포인트 한도(1000000)를 초과할 수 없습니다.");
    }

    @Test
    @DisplayName("포인트 사용 불가 검증")
    void 포인트_사용_불가_검증() {
        // given
        long userId = 7L;
        long currentPoint = 9_000_00L;
        UserPoint userPoint = new UserPoint(userId,currentPoint,System.currentTimeMillis());

        // when
        when(userPointRepository.selectById(userId)).thenReturn(userPoint);

        // then
        assertThatThrownBy(() -> pointService.usePoint(userId,1_000_000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("포인트가 부족하여 차감할 수 없습니다.");
    }
}
