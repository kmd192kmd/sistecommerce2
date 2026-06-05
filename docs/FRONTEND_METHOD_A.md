# 프론트엔드 "방식 A" 구현 가이드

> **방식 A 한 줄 정의:** Thymeleaf가 **빈 껍데기(shell) 페이지**를 그려 주고,
> 그 안의 mount 지점(`<div id="...-root">`)에 **React 번들을 mount** 한다.
> 페이지 단위로 React, **라우팅은 Spring**이 담당.

---

## 1. 역할 분담

| 담당 | 무엇을 |
|------|--------|
| **Spring `@Controller`** | URL 라우팅. 얇게 — shell 템플릿 이름만 반환 |
| **Thymeleaf (서버렌더)** | 페이지 껍데기 + 안 변하는 부분(헤더·푸터·이미지·설명) + mount 지점 |
| **React (클라이언트)** | 껍데기 속 특정 `<div>` 하나에 mount. 실시간·동적 부분만 |
| **REST API (`/api/...`)** | React가 fetch 할 JSON 데이터 |

핵심: **한 페이지를 둘이 나눠 그린다.** 정적 = 타임리프, 동적 = React.

---

## 2. 디렉터리 / 리소스 규약

```
백엔드 (Spring 레포)
  src/main/resources/templates/<도메인>/<page>.html   ← shell (타임리프)
  src/main/resources/static/js/<page>.js              ← React 빌드 산출물
  src/main/java/.../<도메인>/controller/...ViewController.java   ← 페이지 라우팅
  src/main/java/.../<도메인>/controller/...ApiController.java    ← REST API

프론트 (React 프로젝트: 같은 레포 하위 폴더 권장, 예: /frontend)
  /frontend/package.json, vite.config.js
  /frontend/src/<page>.jsx                            ← 진입점(mount 코드)
```

⚠️ **번들은 반드시 `static/` 아래**에 떨어져야 Spring이 서빙해요.
`resources/` 바로 밑 임의 폴더(예: `resources/groupbuy/`)는 뷰 리졸버도 정적 서빙도 안 돼요.

> **React 서버는 없어요.** React는 미리 빌드된 정적 `.js` 파일일 뿐. 배포 때 컨테이너 따로 안 띄움.

---

## 3. 요청 생애주기 (페이지 열릴 때 무슨 일이 일어나나)

```
브라우저 → GET /group-buys/5
  └ Spring @Controller 라우팅 → groupbuy/detail.html (Thymeleaf)
      · 정적 부분(헤더/푸터/이미지/설명)은 서버가 채워 HTML 완성
      · 동적 자리는 빈 mount 지점만: <div id="gb-detail-root" data-gb-id="5">
  ← 완성된 HTML 응답

브라우저가 HTML 파싱 → <script src="/js/groupbuy-detail.js"> 로드
  └ React 번들 실행
      · document.getElementById("gb-detail-root") 찾아 mount
      · data-gb-id="5" 읽음
      · fetch GET /api/group-buys/5 → JSON
      · 그 div 안을 React가 그림 (진행도/카운트다운/매진/대기열 등 실시간)
```

---

## 4. 실제로 만드는 4단계

### 4-1. Thymeleaf shell 작성

헤더/푸터 fragment + mount 지점 + (선택)서버렌더 fallback + 번들 `<script>`.

```html
<!DOCTYPE html>
<html lang="ko" xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{fragments/layout :: head('공동구매 상세')}"></head>
<body>
  <!-- 공통 헤더: A에선 그냥 fragment 그대로 들어감 -->
  <div th:replace="~{fragments/header :: header}"></div>

  <main>
    <!-- 정적: 서버가 채움 -->
    <img th:src="${groupBuy.image}" alt="">

    <!-- ▼ 동적 구매 패널: React mount 지점 ▼ -->
    <!-- data-* 로 초기값(여기선 공구 seq)을 React에 넘김 -->
    <div id="gb-detail-root" th:attr="data-gb-id=${groupBuy.seq}">
      <!-- (선택) React 로드 전/JS 꺼짐 대비 서버렌더 fallback.
           React가 mount되면 이 안을 덮어씀. -->
    </div>
    <!-- ▲ mount 지점 끝 ▲ -->
  </main>

  <div th:replace="~{fragments/footer :: footer}"></div>

  <!-- 번들 로드. 반드시 type="module" -->
  <script type="module" th:src="@{/js/groupbuy-detail.js}"></script>
</body>
</html>
```

> ⚠️ **fallback 주의:** shell 안에서 `${...}`로 쓰는 필드는 **컨트롤러가 모델에 넣어준 것만** 써야 해요.
> 없는 필드를 참조하면 타임리프가 **서버 렌더 중 500 에러** → React가 로드될 기회조차 없어요.
> fallback은 "있는 필드"로만 최소화하거나, 아예 비워두는 게 안전.

### 4-2. React 진입점 작성 (`/frontend/src/groupbuy-detail.jsx`)

```jsx
import React, { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';

function PurchasePanel({ data }) {
  // 진행도, 카운트다운, 옵션 매진, 대기열 등 실시간 UI
  return <div>{data.productName} / {data.finalPrice}원</div>;
}

function App({ id }) {
  const [data, setData] = useState(null);
  useEffect(() => {
    fetch(`/api/group-buys/${id}`)        // ← REST 호출
      .then(r => r.json())
      .then(setData);
  }, [id]);
  if (!data) return <p>불러오는 중…</p>;
  return <PurchasePanel data={data} />;
}

// mount 지점 찾아서 React 띄우기
const el = document.getElementById('gb-detail-root');
if (el) {
  const id = el.dataset.gbId;            // data-gb-id → dataset.gbId (camelCase!)
  createRoot(el).render(<App id={id} />);
}
```

> 📌 `data-gb-id` → JS에선 `el.dataset.gbId` (하이픈이 camelCase로 바뀜).

### 4-3. Vite 빌드 설정 (`/frontend/vite.config.js`)

페이지 단위 번들을 백엔드 `static/js`로 떨어뜨린다.

```js
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';

export default defineConfig({
  plugins: [react()],
  build: {
    // 백엔드 static/js 로 출력 (경로는 실제 레포 구조에 맞게)
    outDir: path.resolve(import.meta.dirname, '../src/main/resources/static/js'),
    emptyOutDir: false,          // static/js 안 다른 페이지 번들 보존
    lib: {
      entry: path.resolve(import.meta.dirname, 'src/groupbuy-detail.jsx'),
      formats: ['es'],           // <script type="module"> 용
      fileName: () => 'groupbuy-detail.js',
    },
  },
});
```

- 빌드: `/frontend` 폴더에서 `npm run build` → `static/js/groupbuy-detail.js` 생성.
- **출력 파일명**이 shell의 `<script th:src="@{/js/groupbuy-detail.js}">` 경로와 **일치해야** 함.
- 페이지가 늘면 `lib.entry`를 객체로 바꿔 진입점별 번들 추가.

### 4-4. Spring 라우트 + REST API

```java
// 페이지 라우팅: shell 템플릿 반환 + 모델 채움
@Controller
@RequiredArgsConstructor
public class GroupBuyViewController {
    private final GroupBuyService service;

    @GetMapping("/group-buys/{seq}")
    public String detail(@PathVariable Long seq, Model model) {
        model.addAttribute("groupBuy", service.getDetail(seq)); // shell이 쓸 데이터
        return "groupbuy/detail";                                // templates/groupbuy/detail.html
    }
}

// 데이터: React가 fetch 할 JSON
@RestController
@RequestMapping("/api/group-buys")
@RequiredArgsConstructor
public class GroupBuyApiController {
    private final GroupBuyService service;

    @GetMapping("/{seq}")
    public GroupBuyDetailResponse detail(@PathVariable Long seq) {
        return service.getDetail(seq);
    }
}
```

---

## 5. 공통 헤더/푸터 — A의 가장 큰 장점

A에선 **모든 페이지가 타임리프 셸**이라, 헤더/푸터를 그냥 fragment로 넣으면 끝:

```html
<div th:replace="~{fragments/header :: header}"></div>
...
<div th:replace="~{fragments/footer :: footer}"></div>
```

C(순수 SPA)였다면 헤더/푸터를 React로 또 만들어야 하지만, **A는 공짜로 공유**돼요.
헤더 안 동적 데이터(로그인 상태·장바구니 수)만 필요하면 그 부분만 작은 React/JS로 처리.

---

## 6. Tailwind 처리

현재 `fragments/layout.html`에서 **Play CDN**(`cdn.tailwindcss.com`) 사용 중.
Play CDN은 런타임에 DOM을 감시해서 **React가 나중에 그린 DOM의 클래스도 자동 스타일링**해요 → A와 잘 맞음.

> 나중에 운영용으로는 CDN 대신 빌드 파이프라인(`@tailwindcss/vite` 등)으로 교체 권장.
> 교체 지점이 `layout.html` 한 곳이라 그때 head fragment만 바꾸면 됨.

---

## 7. 체크리스트 / 흔한 함정

- [ ] 번들 출력 경로 == shell의 `<script th:src>` 경로 (안 맞으면 404, React 안 뜸)
- [ ] `<script>`에 `type="module"` (ES 번들이라 필수)
- [ ] shell의 `${...}` 필드는 **컨트롤러가 넣어준 것만** (없으면 500)
- [ ] `data-*` → JS에선 `dataset.camelCase`
- [ ] 번들은 `static/` 아래 (임의 `resources/` 폴더 금지)
- [ ] 페이지마다 진입점·번들 따로 (page-level React)
- [ ] mount 지점 `id`가 shell과 React 코드에서 동일

---

## 8. 정리

```
GET /page  →  @Controller  →  Thymeleaf shell (헤더/푸터/정적 + 빈 <div id=root data-*>)
                                       ↓ 브라우저
                              <script>로 React 번들 로드 → div에 mount
                                       ↓ fetch
                              GET /api/...  →  JSON  →  React가 동적 부분 렌더
```

- **React 소스**는 `/frontend`(또는 별도 폴더)에서 작성·빌드, **산출물만** `static/js`로.
- **라우팅·공통 chrome·정적 부분**은 Spring/Thymeleaf.
- **실시간·동적 부분**만 React.
