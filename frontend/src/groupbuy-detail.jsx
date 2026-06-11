import React, { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';

// ── 작은 도우미 함수들 ──
// 숫자에 천 단위 콤마 (12345 → "12,345")
const won = (n) => (n ?? 0).toLocaleString('ko-KR');

// 남은 초를 "N일 N시간 N분 N초" 형태로. 0 이하면 "마감".
function formatRemain(sec) {
  if (sec <= 0) return '마감';
  const d = Math.floor(sec / 86400);
  const h = Math.floor((sec % 86400) / 3600);
  const m = Math.floor((sec % 3600) / 60);
  const s = sec % 60;
  if (d > 0) return `${d}일 ${h}시간 ${m}분 ${s}초`;
  return `${h}시간 ${m}분 ${s}초`;
}

/**
 * 공구 구매 패널 (실시간 영역).
 * props.data = 서버(GET /api/group-buys/{id}) 응답, props.id = 공구 seq.
 *
 * useState = "화면이 기억하는 값". 값이 바뀌면 React가 그 부분만 다시 그린다.
 */
function PurchasePanel({ id, data }) {
  const [selectedOption, setSelectedOption] = useState(''); // 사용자가 고른 옵션 seq
  const [remain, setRemain] = useState(data.remainSeconds);  // 남은 초 (1초마다 줄임)
  const [submitting, setSubmitting] = useState(false);       // 참여 요청 중인지
  const [message, setMessage] = useState('');                // 결과 안내 문구

  // useEffect(() => {...}, []) = "이 컴포넌트가 처음 화면에 뜰 때 1번 실행".
  // 여기선 1초마다 남은 초를 1씩 줄이는 타이머를 건다 → 실시간 카운트다운.
  useEffect(() => {
    const timer = setInterval(() => {
      setRemain((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(timer); // 패널이 사라질 때 타이머 정리(메모리 누수 방지)
  }, []);

  const closed = remain <= 0; // 마감됐는지

  // 선택된 옵션 객체 (없으면 null). select의 value는 문자열이라 String으로 맞춰 비교.
  const selected = data.options.find((o) => String(o.optionsSeq) === String(selectedOption)) || null;
  // 화면에 보여줄 가격: 옵션을 고르면 그 옵션의 공구가(기준가+추가금), 아직 안 고르면 공구 기준가.
  const displayPrice = selected ? selected.finalPrice : data.finalPrice;

  // 참여(바로구매) 버튼 클릭 → 서버에 POST
  async function handleParticipate() {
    if (!selectedOption) {
      setMessage('옵션을 선택해주세요.');
      return;
    }
    setSubmitting(true);
    setMessage('');
    try {
      const res = await fetch(
        `/api/group-buys/${id}/participate?optionSeq=${selectedOption}`,
        { method: 'POST' }
      );
      if (res.status === 401) {
        setMessage('로그인이 필요합니다.');
        return;
      }
      if (!res.ok) {
        setMessage('참여 처리에 실패했습니다.');
        return;
      }
      // 서버가 "PARTICIPATED"(정규 참여) 또는 "QUEUED"(매진→대기열)를 돌려준다
      const result = await res.text();
      setMessage(
        result.includes('QUEUED')
          ? '매진되어 대기열에 등록되었습니다.'
          : '공동구매 참여가 완료되었습니다!'
      );
    } catch (e) {
      setMessage('네트워크 오류가 발생했습니다.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div>
      {/* 상품명 + 실시간 마감 카운트다운 */}
      <div className="flex items-start justify-between">
        <h1 className="text-2xl font-bold leading-snug">{data.productName}</h1>
        <span className="shrink-0 bg-slate-900 text-amber-400 text-xs font-bold px-2 py-1">
          {closed ? '마감' : `마감까지 ${formatRemain(remain)}`}
        </span>
      </div>

      {/* 가격: 정가(취소선) / 할인율 / 할인가 */}
      <div className="mt-6 space-y-1 border-y border-gray-100 py-5">
        <p className="text-sm text-gray-400">
          정가 <span className="line-through">{won(data.originalPrice)}원</span>
        </p>
        <p className="flex items-baseline gap-2">
          <span className="text-red-500 font-black text-2xl">{data.discountRate}%</span>
          <span className="text-3xl font-black">{won(displayPrice)}</span>
          <span className="text-lg font-bold">원</span>
        </p>
      </div>

      {/* 공구 진행도 바 */}
      <div className="mt-5">
        <div className="flex justify-between text-xs font-bold mb-1">
          <span className="text-amber-600">{data.currentCount}명 참여</span>
          <span className="text-gray-400">목표 {data.minCount}명</span>
        </div>
        <div className="w-full h-3 bg-gray-100 overflow-hidden">
          {/* style={{ width: ... }} = 진행률 %를 막대 너비로 */}
          <div className="h-full bg-amber-500" style={{ width: `${data.progress}%` }} />
        </div>
      </div>

      {/* 옵션 선택 (매진 옵션은 비활성) */}
      <div className="mt-6">
        <label className="block text-xs font-bold text-gray-500 mb-1">옵션 선택</label>
        <select
          className="w-full border border-gray-300 px-3 py-2.5 text-sm focus:outline-none focus:border-amber-500"
          value={selectedOption}
          onChange={(e) => setSelectedOption(e.target.value)}
          disabled={closed}
        >
          <option value="">옵션을 선택하세요</option>
          {data.options.map((opt) => (
            <option key={opt.optionsSeq} value={opt.optionsSeq} disabled={opt.soldOut}>
              {opt.label}{opt.soldOut ? ' (매진)' : ''}
            </option>
          ))}
        </select>
      </div>

      {/* 참여 버튼 (마감/처리중이면 비활성) */}
      <button
        type="button"
        onClick={handleParticipate}
        disabled={closed || submitting}
        className="mt-4 w-full bg-amber-500 hover:bg-amber-600 text-white font-bold py-3 text-sm disabled:bg-gray-300 disabled:cursor-not-allowed"
      >
        {closed ? '마감되었습니다' : submitting ? '처리 중…' : '바로구매 (공구 참여)'}
      </button>

      {/* 결과 안내 문구 */}
      {message && <p className="mt-2 text-sm text-center text-amber-700 font-bold">{message}</p>}

      <p className="mt-2 text-[11px] text-gray-400">
        ※ 공구 상품은 쿠폰·핫딜 적용 불가 / 1인 1상품, 수량 1개 고정
      </p>
    </div>
  );
}

/**
 * 최상위: 마운트되면 공구 데이터를 fetch해서 패널에 넘긴다.
 * data가 아직 없으면(로딩 중) "불러오는 중…"만 보여준다.
 */
function App({ id }) {
  const [data, setData] = useState(null);
  useEffect(() => {
    fetch(`/api/group-buys/${id}`)
      .then((r) => r.json())
      .then(setData);
  }, [id]);
  if (!data) return <p className="text-gray-400">불러오는 중…</p>;
  return <PurchasePanel id={id} data={data} />;
}

// HTML의 <div id="gb-detail-root" data-gb-id="7"> 자리에 React를 끼워넣는다.
const el = document.getElementById('gb-detail-root');
if (el) {
  const id = el.dataset.gbId; // data-gb-id → dataset.gbId (camelCase 자동 변환)
  createRoot(el).render(<App id={id} />);
}
