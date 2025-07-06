# 🔄 PAPIA GitFlow 워크플로우 (한글 커밋/PR 가이드)

이 문서는 PAPIA 프로젝트의 GitFlow 워크플로우를 정의하며, 모든 커밋 메시지와 PR 제목, 설명은 반드시 **한글**로 작성해야 합니다.

## 📋 브랜치 전략

### 🌿 메인 브랜치
- **`main`** - 프로덕션 릴리스 브랜치
- **`develop`** - 개발 통합 브랜치

### 🔧 기능 브랜치
- **`feature/기능명`** - 새로운 기능 개발
- **`hotfix/버그명`** - 긴급 버그 수정
- **`release/버전명`** - 릴리스 준비

## 🚀 GitFlow 워크플로우

### 1. 초기 설정

```bash
# 메인 브랜치에서 시작
git checkout main
git pull origin main

# develop 브랜치 생성 (최초 1회)
git checkout -b develop
git push -u origin develop
```

### 2. 기능 개발 (Feature Development)

```bash
# develop 브랜치에서 기능 브랜치 생성
git checkout develop
git pull origin develop
git checkout -b feature/생리-기록-추가

# 개발 작업 수행
# ... 코드 작성 ...

# 커밋 메시지 예시 (한글로 작성)
git commit -m "feat: 생리 기록 기능 추가\n\n- 생리 시작/종료일 기록 기능 구현\n- 생리 강도 선택 기능 추가\n- 생리 이력 화면 생성\n- 생리 통계 계산 기능 추가\n\n관련 이슈: #123"
```

### 3. 커밋 메시지 규칙

#### 📝 커밋 타입
- **`feat:`** - 새로운 기능 추가
- **`fix:`** - 버그 수정
- **`docs:`** - 문서 수정
- **`style:`** - 코드 포맷팅 (기능 변경 없음)
- **`refactor:`** - 코드 리팩토링
- **`test:`** - 테스트 추가/수정
- **`chore:`** - 빌드/설정 변경

#### 📋 커밋 메시지 구조
```
type(영역): 한글로 간단 요약

상세 설명 (한글)

관련 이슈: #번호
```

#### 💡 예시 (모두 한글)
```bash
# 기능 추가
git commit -m "feat(캘린더): 생리 예측 알고리즘 추가\n\n- 평균 주기 계산 기능 구현\n- 배란일 예측 기능 추가\n- 가임기 하이라이트 표시\n- 불규칙 주기 감지 기능\n\n관련 이슈: #45"

# 버그 수정
git commit -m "fix(DB): Room 마이그레이션 오류 수정\n\n- PeriodRecord 엔티티 스키마 수정\n- 마이그레이션 전략 추가\n- 앱 업데이트 시 데이터 손실 방지\n\n관련 이슈: #67"

# 문서 업데이트
git commit -m "docs(README): 설치 가이드 업데이트\n\n- Android Studio 설정법 추가\n- 의존성 설치 방법 추가\n- 문제 해결 섹션 추가\n\n관련 이슈: #89"
```

### 4. 브랜치 네이밍 규칙

#### 🎯 기능 브랜치
```
feature/기능명-간단설명
feature/생리-기록
feature/사용자-프로필
feature/증상-로그
```

#### 🐛 핫픽스 브랜치
```
hotfix/버그명-간단설명
hotfix/앱-시작-크래시
hotfix/데이터-손실
hotfix/달력-표시-오류
```

#### 🏷️ 릴리스 브랜치
```
release/버전명
release/v1.2.0
release/v1.3.0-beta
```

### 5. Pull Request 규칙

#### 📋 PR 제목 규칙 (반드시 한글)
```
type(영역): 한글로 간단 설명

예시:
feat(캘린더): 생리 예측 기능 추가
fix(DB): 마이그레이션 오류 해결
docs(설정): 설치 가이드 업데이트
```

#### 📝 PR 설명 템플릿 (한글)
```markdown
## 📋 변경 사항
- [ ] 새로운 기능 추가
- [ ] 버그 수정
- [ ] 문서 업데이트
- [ ] 코드 리팩토링

## 🔍 상세 내용
변경된 내용을 한글로 상세히 작성해주세요.

## 🧪 테스트
- [ ] 단위 테스트 통과
- [ ] 통합 테스트 통과
- [ ] UI 테스트 완료

## 📸 스크린샷 (UI 변경 시)
변경된 UI의 스크린샷을 첨부해주세요.

## 🔗 관련 이슈
관련 이슈: #번호
```

### 6. 브랜치 병합 규칙

#### 🔄 Feature → Develop
```bash
# 기능 개발 완료 후
git checkout develop
git pull origin develop
git merge --no-ff feature/생리-기록
git push origin develop
git branch -d feature/생리-기록
```

#### 🏷️ Develop → Release
```bash
# 릴리스 준비
git checkout -b release/v1.2.0 develop
# 버전 번호 업데이트, 최종 테스트
git commit -m "chore: v1.2.0 릴리스 준비"
git push origin release/v1.2.0
```

#### 🚀 Release → Main
```bash
# 릴리스 완료
git checkout main
git merge --no-ff release/v1.2.0
git tag -a v1.2.0 -m "v1.2.0 릴리스"
git push origin main --tags
```

#### 🔧 Hotfix → Main
```bash
# 긴급 수정
git checkout -b hotfix/치명적-버그 main
# 버그 수정
git commit -m "fix: 치명적 데이터 손실 문제 해결"
git checkout main
git merge --no-ff hotfix/치명적-버그
git tag -a v1.2.1 -m "v1.2.1 핫픽스"
git push origin main --tags
```

## 🛠️ 개발 도구 설정

### 📝 Git Hooks 설정
```bash
# .git/hooks/pre-commit
#!/bin/sh
# 커밋 전 코드 포맷팅 및 린팅
./gradlew ktlintCheck
./gradlew detekt
```

### 🔍 Git Aliases
```bash
# ~/.gitconfig
[alias]
    st = status
    co = checkout
    br = branch
    ci = commit
    lg = log --oneline --graph --decorate
    unstage = reset HEAD --
    last = log -1 HEAD
```

## 📊 브랜치 보호 규칙

### 🛡️ Main 브랜치 보호
- **필수 상태 검사**: CI/CD 파이프라인 통과 필수
- **PR 리뷰 필수**: 최소 1명 이상 승인
- **직접 푸시 금지**
- **선형 히스토리 유지**

### 🔒 Develop 브랜치 보호
- **필수 상태 검사**: 빌드 및 테스트 통과
- **PR 리뷰 필수**
- **새 커밋 시 리뷰 무효화**

## 🚨 금지 사항

### ❌ 하지 말아야 할 것들
- **직접 main 브랜치에 푸시**
- **긴 커밋 메시지** (50자 이내 권장)
- **한 커밋에 여러 기능 포함**
- **영어 커밋 메시지** (반드시 한글로 작성)
- **브랜치를 삭제하지 않고 방치**

### ✅ 권장 사항
- **작은 단위로 자주 커밋**
- **명확한 한글 커밋 메시지 작성**
- **PR 리뷰 시 상세한 한글 설명**
- **브랜치 정리 정기적으로 수행**
- **태그를 사용한 버전 관리**

## 📈 모니터링 및 메트릭

### 📊 추적할 지표
- **브랜치 수명**: feature 브랜치 평균 수명
- **리뷰 시간**: PR 리뷰 평균 소요 시간
- **배포 빈도**: 릴리스 주기
- **버그 발생률**: hotfix 빈도

### 🔍 정기 점검
- **주간**: 브랜치 정리 및 점검
- **월간**: 워크플로우 개선 검토
- **분기별**: 전체 프로세스 평가

## 🆘 문제 해결

### 🔧 일반적인 문제들

#### 브랜치 충돌 해결
```bash
git checkout develop
git pull origin develop
git checkout feature/내-기능
git rebase develop
# 충돌 해결 후
git rebase --continue
```

#### 실수로 잘못된 브랜치에 커밋
```bash
# 커밋을 올바른 브랜치로 이동
git cherry-pick <커밋해시>
git reset --hard HEAD~1  # 원래 브랜치에서 제거
```

#### 커밋 메시지 수정
```bash
# 마지막 커밋 메시지 수정
git commit --amend -m "새로운 커밋 메시지"

# 이전 커밋 메시지 수정
git rebase -i HEAD~3
```

---

## 📞 지원

GitFlow 관련 질문이나 문제가 있으시면 개발팀에 연락해 주세요.

**마지막 업데이트**: 2024년 12월  
**담당자**: Sangwoo  
**버전**: 1.0.1 