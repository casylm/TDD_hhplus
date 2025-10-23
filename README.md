## 동시성 제어 방식 분석 보고서

### [1] 문제 정의
#### **1) 여러 사용자의 동시 요청**
- 여러 사용자가 동시에 포인트 데이터를 수정하려고 접근할 경우
  하나의 자원(공유 데이터)에 대해 경쟁 상태(Race Condition) 발생 가능
- 별도의 동기화 처리가 없으면 포인트 값이 덮어써지거나 손실될 위험 존재

#### **2) 동일 사용자의 데이터 무결성**
- 동일한 사용자가 거의 동시에 여러 번 포인트 충전 또는 사용 요청을 보낼 경우
  첫 번째 요청이 완료되기 전에 다른 요청이 실행되어 일관성 문제 발생

### [2] 고려 기술

#### 1. synchronized

- 구현이 간단하고 직관적
- `Global Lock` 으로 동작하기 때문에, 한 사용자의 작업이 다른 모든 사용자의 요청까지 차단
- 특정 사용자 A의 충전이 끝날 때까지 사용자 B의 충전도 대기해야 하는 비효율 발생
<br>

#### 2. ConcurrentHashMap<Long, ReentrantLock>

- 동일 사용자(userId) 에 대해서만 동시성 제어
- 작업 후, `finally` 블록에서 `unlock()` 수행

### [3] 적용 결과

#### 1. 사용자 단위 락 획득

```
Lock lock = getUserLock(id);
lock.lock();
```
- ConcurrentHashMap<Long, Lock>을 이용해 사용자별로 독립적인 락을 관리
<br><br>
#### 2. 포인트 조회(주요 기능)

```
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
}
```
<br>

#### 3. 락 해제 (Unlock)

``` 
finally {
            lock.unlock();
        }
```
- 예외 발생 여부와 관계없이 락 해제

- 다음 요청이 동일 사용자 자원에 접근할 수 있도록 교착상태(Deadlock) 방지

### [4] 결론

프로젝트를 통해 TDD와 동시성 제어의 핵심 개념을 학습하며,
포인트 충전·사용 과정에서 발생할 수 있는 정책(예외 처리)을 경험했습니다.<br>
동시에, 데이터 충돌과 순차성 문제를 분석하고 사용자 단위 락 관리로 해결 방안을 도출함으로써
안정적이고 확장 가능한 시스템 구현의 중요성을 이해할 수 있었습니다.