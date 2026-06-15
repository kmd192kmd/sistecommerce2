import { useEffect, useRef, useState } from "react";
import Chart from "chart.js/auto";
import { createRoot } from "react-dom/client";

function SalesDashboard() {
  const canvasRef = useRef(null);
  const chartRef = useRef(null);

  const [period, setPeriod] = useState("7d");
  const [labels, setLabels] = useState([]);
  const [salesData, setSalesData] = useState([]);

const createGradient = (ctx, chartArea) => {
  const gradient = ctx.createLinearGradient(0, chartArea.top, 0, chartArea.bottom);

  gradient.addColorStop(0, "rgba(54, 162, 235, 0.7)");
  gradient.addColorStop(1, "rgba(54, 162, 235, 0.05)");

  return gradient;
};

  const fetchData = async (selectedPeriod) => {
    const res = await fetch(
      `/api/admin/statistics/sales-trend?period=${selectedPeriod}`
    );
    const data = await res.json();

    setLabels(data.map((i) => i.label));
    setSalesData(data.map((i) => i.sales));
  };

  useEffect(() => {
    fetchData(period);
  }, [period]);

  useEffect(() => {
    if (!canvasRef.current) return;

    const ctx = canvasRef.current.getContext("2d");

    // 기존 차트 제거
    if (chartRef.current) {
      chartRef.current.destroy();
    }

    chartRef.current = new Chart(ctx, {
      type: "line",
      data: {
        labels,
        datasets: [
  {
    label: "매출",
    data: salesData,
    tension: 0.3,
    fill: true,

    backgroundColor: (context) => {
      const chart = context.chart;
      const { ctx, chartArea } = chart;

      if (!chartArea) return null;

      return createGradient(ctx, chartArea);
    },
  },
],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false, // ⭐ 핵심 (레이아웃 정상화)
        plugins: {
          legend: {
            display: false,
          },
        },
        scales: {
          y: {
            beginAtZero: true,
          },
        },
      },
    });

    return () => chartRef.current?.destroy();
  }, [labels, salesData]);

  return (
    <div className="card">
      {/* 헤더 (Thymeleaf랑 동일 구조) */}
      <div className="flex justify-between items-center mb-6">
        <h4 className="text-title">매출 추이</h4>

        <select
          value={period}
          onChange={(e) => setPeriod(e.target.value)}
          className="form-input w-40"
        >
          <option value="7d">최근 7일</option>
          <option value="15d">최근 15일</option>
          <option value="month">이번 달</option>
          <option value="3m">최근 3개월</option>
        </select>
      </div>

      {/* 차트 영역 */}
      <div className="h-[400px] w-full">
        <canvas ref={canvasRef}></canvas>
      </div>
    </div>
  );
}

const el = document.getElementById("sales-dashboard-root");

if (el) {
  createRoot(el).render(<SalesDashboard />);
}