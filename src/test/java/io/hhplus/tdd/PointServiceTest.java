package io.hhplus.tdd;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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
}
