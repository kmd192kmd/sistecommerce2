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