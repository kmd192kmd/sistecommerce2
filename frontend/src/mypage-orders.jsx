import React, { useState, useEffect, useRef } from 'react';
import { createRoot } from 'react-dom/client';

function App() {
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    
    // 검색 및 정렬 파라미터 상태
    const [keyword, setKeyword] = useState(() => {
        const params = new URLSearchParams(window.location.search);
        return params.get('keyword') || '';
    });
    const [period, setPeriod] = useState(() => {
        const params = new URLSearchParams(window.location.search);
        return params.get('period') || '6months';
    });

    // 배송조회 모달 상태
    const [isTrackOpen, setIsTrackOpen] = useState(false);
    const [trackInfo, setTrackInfo] = useState({
        trackingNumber: '',
        status: 'READY',
        companyName: ''
    });
    
    // 모달창 닫기 애니메이션 잔상 개선용 UI 리셋 지연 관리
    const [trackModalUI, setTrackModalUI] = useState({
        trackingNumber: '-',
        status: 'READY',
        companyName: '',
        stepProgressWidth: '0%',
        step1Active: false,
        step2Active: false,
        step3Active: false
    });
    const trackResetTimeoutRef = useRef(null);

    // 경고 모달 상태
    const [isCancelAlertOpen, setIsCancelAlertOpen] = useState(false);
    const [isReturnNoSelectionOpen, setIsReturnNoSelectionOpen] = useState(false);

    // 취소/반품 확인 모달 상태
    const [confirmModal, setConfirmModal] = useState({
        isOpen: false,
        type: '', // 'CANCEL' or 'RETURN'
        title: '',
        text: '',
        step: 'CONFIRM', // 'CONFIRM', 'PROCESSING', 'RESULT'
        isSuccess: false,
        resultTitle: '',
        resultMessage: '',
        payload: {}
    });

    // 다중 선택 상태 (반품용 체크박스)
    const [selectedItems, setSelectedItems] = useState({});

    // API 호출을 통해 데이터 가져오기
    const fetchOrders = async (currentKeyword, currentPeriod) => {
        setLoading(true);
        try {
            const params = new URLSearchParams();
            if (currentKeyword) params.append('keyword', currentKeyword);
            if (currentPeriod) params.append('period', currentPeriod);
            
            const response = await fetch(`/api/mypage/orders?${params.toString()}`);
            if (!response.ok) throw new Error("데이터 수집 실패");
            const data = await response.json();
            setOrders(data);
        } catch (error) {
            console.error("주문목록 로드 중 에러: ", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchOrders(keyword, period);
    }, []);

    // 검색 실행
    const handleSearchSubmit = (e) => {
        e.preventDefault();
        // 주소창 업데이트 (뒤로가기/년도 조건 연동 유지용)
        const params = new URLSearchParams(window.location.search);
        if (keyword) params.set('keyword', keyword);
        else params.delete('keyword');
        params.set('period', period);
        window.history.replaceState({}, '', `${window.location.pathname}?${params.toString()}`);
        
        fetchOrders(keyword, period);
    };

    // 기간 변경
    const handlePeriodChange = (newPeriod) => {
        setPeriod(newPeriod);
        const params = new URLSearchParams(window.location.search);
        params.set('period', newPeriod);
        if (keyword) params.set('keyword', keyword);
        else params.delete('keyword');
        window.history.replaceState({}, '', `${window.location.pathname}?${params.toString()}`);
        
        fetchOrders(keyword, newPeriod);
    };

    // 배송조회 모달 열기
    const openTrackModal = (trackingNumber, status, companyName) => {
        if (trackResetTimeoutRef.current) {
            clearTimeout(trackResetTimeoutRef.current);
            trackResetTimeoutRef.current = null;
        }

        const currentStatus = status || 'READY';
        
        // 게이지 진행폭 및 활성화 상태 계산
        let width = '0%';
        let s1 = false, s2 = false, s3 = false;

        if (currentStatus !== 'CANCELED' && currentStatus !== 'FAILED') {
            if (currentStatus === 'READY') {
                s1 = true;
                width = '0%';
            } else if (currentStatus === 'SHIPPING' || currentStatus === 'DELAYED') {
                s1 = true;
                s2 = true;
                width = '50%';
            } else if (currentStatus === 'DELIVERED') {
                s1 = true;
                s2 = true;
                s3 = true;
                width = '100%';
            }
        }

        setTrackInfo({ trackingNumber, status: currentStatus, companyName });
        setTrackModalUI({
            trackingNumber: trackingNumber || '발급대기',
            status: currentStatus,
            companyName: companyName ? `(${companyName})` : '',
            stepProgressWidth: width,
            step1Active: s1,
            step2Active: s2,
            step3Active: s3
        });
        setIsTrackOpen(true);
    };

    // 배송조회 모달 닫기 (150ms 닫기 애니메이션 동기화 및 OOM 방지)
    const closeTrackModal = () => {
        setIsTrackOpen(false);
        
        if (trackResetTimeoutRef.current) {
            clearTimeout(trackResetTimeoutRef.current);
        }
        
        trackResetTimeoutRef.current = setTimeout(() => {
            setTrackModalUI({
                trackingNumber: '-',
                status: 'READY',
                companyName: '',
                stepProgressWidth: '0%',
                step1Active: false,
                step2Active: false,
                step3Active: false
            });
            trackResetTimeoutRef.current = null;
        }, 150);
    };

    // 반품 선택 체크박스 변경
    const handleCheckboxChange = (orderItemSeq, checked) => {
        setSelectedItems(prev => ({
            ...prev,
            [orderItemSeq]: checked
        }));
    };

    // 주문 취소 로직 실행
    const executeOrderCancel = async () => {
        setConfirmModal(prev => ({ ...prev, step: 'PROCESSING' }));
        const { orderSeq, orderItemSeqList } = confirmModal.payload;
        try {
            const response = await fetch(`/api/mypage/orders/${orderSeq}/cancel-items`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Accept": "application/json"
                },
                body: JSON.stringify({
                    orderItemSeqList,
                    cancelReason: "고객 주문 취소 (택배사 묶음 전체)"
                })
            });

            const result = await response.json();

            if (!response.ok) {
                setConfirmModal(prev => ({
                    ...prev,
                    step: 'RESULT',
                    isSuccess: false,
                    resultTitle: '취소 실패',
                    resultMessage: result?.message || '주문 취소에 실패했습니다.'
                }));
                return;
            }

            setConfirmModal(prev => ({
                ...prev,
                step: 'RESULT',
                isSuccess: true,
                resultTitle: '취소 완료',
                resultMessage: result?.message || '주문 취소가 완료되었습니다.'
            }));
        } catch (error) {
            console.error(error);
            setConfirmModal(prev => ({
                ...prev,
                step: 'RESULT',
                isSuccess: false,
                resultTitle: '오류',
                resultMessage: '주문 취소 요청 중 오류가 발생했습니다.'
            }));
        }
    };

    // 반품 신청 로직 실행
    const executeOrderReturn = async () => {
        setConfirmModal(prev => ({ ...prev, step: 'PROCESSING' }));
        const { orderSeq, orderItemSeqList } = confirmModal.payload;
        try {
            const response = await fetch(`/api/mypage/orders/${orderSeq}/return`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Accept": "application/json"
                },
                body: JSON.stringify({
                    orderItemSeqList,
                    cancelReason: "고객 반품 신청"
                })
            });

            const result = await response.json();

            if (!response.ok) {
                setConfirmModal(prev => ({
                    ...prev,
                    step: 'RESULT',
                    isSuccess: false,
                    resultTitle: '반품 실패',
                    resultMessage: result?.message || '반품 신청에 실패했습니다.'
                }));
                return;
            }

            setConfirmModal(prev => ({
                ...prev,
                step: 'RESULT',
                isSuccess: true,
                resultTitle: '반품 완료',
                resultMessage: result?.message || '반품 신청이 접수되었습니다.'
            }));
        } catch (error) {
            console.error(error);
            setConfirmModal(prev => ({
                ...prev,
                step: 'RESULT',
                isSuccess: false,
                resultTitle: '오류',
                resultMessage: '반품 신청 요청 중 오류가 발생했습니다.'
            }));
        }
    };

    // 주문 취소 버튼 클릭 시 모달 띄우기
    const handleOrderCancelClick = (orderSeq, deliveryGroup) => {
        if (deliveryGroup.deliveryStatus !== 'READY') {
            setIsCancelAlertOpen(true);
            return;
        }

        const activeItems = deliveryGroup.items.filter(item => item.itemStatus !== 6);
        const orderItemSeqList = activeItems.map(item => item.orderItemSeq);

        if (orderItemSeqList.length === 0) {
            return; // 취소 가능 물품 없음
        }

        const firstItemName = activeItems[0].name;
        const displayTitle = orderItemSeqList.length > 1 
            ? `[${firstItemName} 외 ${orderItemSeqList.length - 1}건]` 
            : `[${firstItemName}]`;

        setConfirmModal({
            isOpen: true,
            type: 'CANCEL',
            title: '주문 취소 확인',
            text: `${displayTitle}을 정말 주문 취소하시겠습니까?<br/><br/><span class="text-xs text-red-500 font-bold">※ 배송 준비 중이므로 즉시 환불 처리됩니다.</span>`,
            step: 'CONFIRM',
            isSuccess: false,
            resultTitle: '',
            resultMessage: '',
            payload: { orderSeq, orderItemSeqList }
        });
    };

    // 반품 신청 버튼 클릭 시 모달 띄우기
    const handleOrderReturnClick = (orderSeq, deliveryGroup) => {
        // 이 묶음에서 체크된 항목 추출
        const orderItemSeqList = deliveryGroup.items
            .map(item => item.orderItemSeq)
            .filter(seq => selectedItems[seq] === true);

        if (orderItemSeqList.length === 0) {
            setIsReturnNoSelectionOpen(true);
            return;
        }

        const activeItems = deliveryGroup.items.filter(item => orderItemSeqList.includes(item.orderItemSeq));
        const firstItemName = activeItems[0].name;
        const displayTitle = orderItemSeqList.length > 1 
            ? `[${firstItemName} 외 ${orderItemSeqList.length - 1}건]` 
            : `[${firstItemName}]`;

        setConfirmModal({
            isOpen: true,
            type: 'RETURN',
            title: '반품 신청 확인',
            text: `${displayTitle}을 정말 반품 신청하시겠습니까?<br/><br/><span class="text-xs text-amber-500 font-bold">※ 반품 신청 완료 후 택배 수거 및 검수가 진행됩니다.</span>`,
            step: 'CONFIRM',
            isSuccess: false,
            resultTitle: '',
            resultMessage: '',
            payload: { orderSeq, orderItemSeqList }
        });
    };

    return (
        <section class="space-y-6">
            <div class="flex justify-between items-end mb-8">
                <div>
                    <h1 class="text-2xl font-extrabold text-slate-900">주문목록 / 배송조회</h1>
                    <p class="text-slate-500 mt-2 text-sm">하나의 주문 안에서 택배사별 상품 묶음을 확인할 수 있습니다.</p>
                </div>
            </div>

            {/* 검색 및 필터 박스 */}
            <div class="bg-white rounded-2xl p-6 shadow-sm border border-slate-200">
                <form onSubmit={handleSearchSubmit} class="flex items-center gap-3">
                    <div class="relative flex-1">
                        <svg class="w-5 h-5 absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                        </svg>
                        <input type="text" value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="주문하신 상품명을 검색해보세요"
                               class="w-full pl-12 pr-4 py-3 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-amber-500 focus:bg-white transition-all" />
                    </div>
                    <button type="submit" class="bg-amber-500 hover:bg-amber-600 text-white px-8 py-3 rounded-xl text-sm font-bold transition-colors shadow-sm shadow-amber-200">검색</button>
                </form>

                {/* 최근 6개월 및 연도별 탭 */}
                <div class="flex flex-wrap gap-2 text-xs font-semibold mt-4">
                    <button onClick={() => handlePeriodChange('6months')}
                       className={`px-4 py-2 rounded-lg transition-all ${period === '6months' ? 'bg-slate-800 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}`}>최근 6개월</button>
                    
                    {[2026, 2025, 2024].map(year => (
                        <button key={year} onClick={() => handlePeriodChange(String(year))}
                           className={`px-4 py-2 rounded-lg transition-all ${period === String(year) ? 'bg-slate-800 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}`}>{year}</button>
                    ))}
                </div>
            </div>

            {/* 주문 목록 카드 영역 */}
            <div id="orderListContainer">
                {loading ? (
                    <div class="bg-white p-12 text-center border border-slate-200 rounded-2xl shadow-sm">
                        <p class="text-slate-500 text-sm">주문 정보를 불러오는 중입니다...</p>
                    </div>
                ) : orders && orders.length > 0 ? (
                    <div class="space-y-5">
                        {orders.map(o => (
                            <article key={o.orderSeq} class="premium-card overflow-hidden">
                                <div class="px-6 py-4 bg-slate-50/50 border-b border-slate-100 flex justify-between items-center">
                                    <div class="text-sm flex items-center gap-3">
                                        <span class="text-slate-500 font-medium">주문일자</span>
                                        <span class="font-bold text-slate-800">{o.orderDate}</span>
                                    </div>
                                    <a href={`/mypage/orders/${o.orderSeq}`} class="text-xs font-bold text-amber-600 hover:text-amber-700">주문 상세보기</a>
                                </div>

                                <div class="bg-slate-50/60 px-3 py-4 sm:px-6 sm:py-5 space-y-4">
                                    {o.deliveries.map((d, dIdx) => {
                                        // 묶음 안의 모든 상품이 반품/취소 상태인지 판별
                                        const activeItems = d.items.filter(item => ![6,7,8,9].includes(item.itemStatus));
                                        const hasActiveItems = activeItems.length > 0;
                                        
                                        // 묶음 안의 모든 상품이 취소 상태(6)인지 판별
                                        const cancelActiveItems = d.items.filter(item => item.itemStatus !== 6);
                                        const hasCancelActiveItems = cancelActiveItems.length > 0;

                                        return (
                                            <div key={dIdx} class="delivery-company-group p-5 sm:p-6" data-order-seq={o.orderSeq} data-company-name={d.companyName}>
                                                <div class="mb-5 flex flex-col gap-2 border-b border-slate-100 pb-4 sm:flex-row sm:items-center sm:justify-between">
                                                    <div class="flex flex-wrap items-center gap-2">
                                                        {d.deliveryStatus === 'DELIVERED' && (
                                                            <>
                                                                <span class="status-badge status-DELIVERED">배송완료</span>
                                                                {d.completedAt && <span class="text-xs font-semibold text-slate-500">{`도착일 ${d.completedAt}`}</span>}
                                                            </>
                                                        )}
                                                        {d.deliveryStatus === 'CANCELED' && <span class="status-badge status-CANCELED">주문취소</span>}
                                                        {(d.deliveryStatus === 'SHIPPING' || d.deliveryStatus === 'DELAYED') && <span class="status-badge status-SHIPPING">배송중</span>}
                                                        {d.deliveryStatus === 'READY' && <span class="status-badge status-READY">배송대기</span>}
                                                    </div>
                                                </div>

                                                <div class="flex flex-col lg:flex-row gap-8">
                                                    <div class="flex-1 space-y-4">
                                                        {d.items.map(item => (
                                                            <div key={item.orderItemSeq} 
                                                                 class={`cancel-item-target flex gap-5 items-center pb-4 last:pb-0 last:border-b-0 border-b border-slate-100 ${[6,7,8,9].includes(item.itemStatus) ? ' opacity-60' : ''}`}
                                                                 data-order-item-seq={item.orderItemSeq}
                                                                 data-item-status={item.itemStatus}>
                                                                
                                                                {d.deliveryStatus === 'DELIVERED' && ![6,7,8,9].includes(item.itemStatus) && (
                                                                    <input type="checkbox"
                                                                           checked={!!selectedItems[item.orderItemSeq]}
                                                                           onChange={(e) => handleCheckboxChange(item.orderItemSeq, e.target.checked)}
                                                                           class="return-item-check w-5 h-5 accent-amber-500 shrink-0" />
                                                                )}

                                                                <a href={`/products/${item.productSeq}`} class="shrink-0">
                                                                    <div class="relative w-20 h-20 rounded-xl overflow-hidden shadow-sm border border-slate-100 hover:opacity-90 transition-opacity">
                                                                        <img src={item.image} alt="상품 이미지" class={`w-full h-full object-cover ${[6,7,8,9].includes(item.itemStatus) ? ' grayscale' : ''}`} />
                                                                        {item.itemStatus === 6 && (
                                                                            <div class="absolute inset-0 bg-slate-900/45 flex items-center justify-center">
                                                                                <span class="text-[11px] font-black text-white border border-white/60 px-2 py-1 rounded">취소됨</span>
                                                                            </div>
                                                                        )}
                                                                        {[7, 8].includes(item.itemStatus) && (
                                                                            <div class="absolute inset-0 bg-slate-900/45 flex items-center justify-center">
                                                                                <span class="text-[11px] font-black text-white border border-white/60 px-2 py-1 rounded">반품대기</span>
                                                                            </div>
                                                                        )}
                                                                        {item.itemStatus === 9 && (
                                                                            <div class="absolute inset-0 bg-slate-900/45 flex items-center justify-center">
                                                                                <span class="text-[11px] font-black text-white border border-white/60 px-2 py-1 rounded">반품완료</span>
                                                                            </div>
                                                                        )}
                                                                    </div>
                                                                </a>

                                                                <div class="flex flex-col justify-center">
                                                                    <div class="flex items-center gap-2">
                                                                        <a href={`/products/${item.productSeq}`}>
                                                                            <h4 class={`font-bold text-base leading-tight transition-colors ${[6,7,8,9].includes(item.itemStatus) ? ' text-slate-400 line-through' : ' text-slate-900 hover:text-amber-600'}`}>
                                                                                {item.name}
                                                                            </h4>
                                                                        </a>
                                                                        {item.itemStatus === 7 && <span class="text-[11px] font-bold bg-amber-50 text-amber-600 border border-amber-100 px-2 py-0.5 rounded-full">반품요청</span>}
                                                                        {item.itemStatus === 8 && <span class="text-[11px] font-bold bg-amber-50 text-amber-600 border border-amber-100 px-2 py-0.5 rounded-full">반품진행중</span>}
                                                                        {item.itemStatus === 9 && <span class="text-[11px] font-bold bg-slate-100 text-slate-600 border border-slate-200 px-2 py-0.5 rounded-full">반품완료</span>}
                                                                    </div>

                                                                    <div class="mt-1.5 text-sm text-slate-500 font-medium">
                                                                        <span class={`font-bold ${[6,7,8,9].includes(item.itemStatus) ? ' text-slate-400 line-through' : ' text-slate-700'}`}>
                                                                            {item.price ? `${item.price.toLocaleString()}원` : '0원'}
                                                                        </span>
                                                                        <span class="mx-1">x</span>
                                                                        <span>{item.qty}개</span>
                                                                    </div>

                                                                    {item.itemStatus === 6 && <p class="text-xs text-red-500 font-semibold mt-1">주문 취소가 완료된 상품입니다.</p>}
                                                                    {item.itemStatus === 7 && <p class="text-xs text-amber-600 font-semibold mt-1">반품 신청이 접수되어 상품 수거 대기 중입니다.</p>}
                                                                    {item.itemStatus === 8 && <p class="text-xs text-amber-600 font-semibold mt-1">상품 수거 완료 후 검수가 진행 중입니다.</p>}
                                                                    {item.itemStatus === 9 && <p class="text-xs text-slate-500 font-semibold mt-1">반품 및 환불 처리가 완료되었습니다.</p>}
                                                                    {item.itemStatus === 0 && d.deliveryStatus !== 'READY' && <p class="text-xs text-slate-400 mt-1">배송대기 상태에서만 취소할 수 있습니다.</p>}
                                                                </div>
                                                            </div>
                                                        ))}
                                                    </div>

                                                    <div class="w-full lg:w-72 shrink-0 flex flex-col justify-center pl-0 lg:pl-6 border-t lg:border-t-0 lg:border-l border-slate-100 pt-5 lg:pt-0">
                                                        <div class="flex flex-col gap-2 w-full">
                                                            <button type="button"
                                                                    onClick={() => openTrackModal(d.trackingNumber, d.deliveryStatus, d.companyName)}
                                                                    class="btn-action py-2.5 border border-slate-200 text-slate-700 bg-white hover:border-amber-500 hover:text-amber-600 text-sm shadow-sm">
                                                                배송조회
                                                            </button>

                                                            {/* 반품 버튼 분기 */}
                                                            {!hasActiveItems ? (
                                                                <button type="button" disabled class="btn-action py-2.5 border border-slate-200 text-slate-400 bg-slate-50 text-sm shadow-sm cursor-not-allowed">반품 불가</button>
                                                            ) : (
                                                                <button type="button"
                                                                        onClick={() => handleOrderReturnClick(o.orderSeq, d)}
                                                                        className={`btn-action py-2.5 border text-sm shadow-sm transition-all ${d.deliveryStatus === 'DELIVERED' ? 'border-amber-200 text-amber-600 bg-amber-50 hover:bg-amber-100' : 'border-slate-200 text-slate-500 bg-white hover:bg-slate-50'}`}>
                                                                    반품신청
                                                                </button>
                                                            )}

                                                            {/* 취소 버튼 분기 */}
                                                            {!hasCancelActiveItems ? (
                                                                <button type="button" disabled class="btn-action py-2.5 border border-slate-200 text-slate-400 bg-slate-50 text-sm shadow-sm cursor-not-allowed">취소 완료</button>
                                                            ) : (
                                                                <button type="button"
                                                                        onClick={() => handleOrderCancelClick(o.orderSeq, d)}
                                                                        className={`btn-action py-2.5 border text-sm shadow-sm transition-all ${d.deliveryStatus === 'READY' ? 'border-red-200 text-red-600 bg-red-50 hover:bg-red-100' : 'border-slate-200 text-slate-500 bg-white hover:bg-slate-50'}`}>
                                                                    주문취소
                                                                </button>
                                                            )}
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                        );
                                    })}
                                </div>
                            </article>
                        ))}
                    </div>
                ) : (
                    <div class="premium-card p-12 text-center border border-slate-200 rounded-2xl shadow-sm bg-white">
                        <div class="w-20 h-20 bg-slate-100 rounded-full flex items-center justify-center mx-auto mb-4">
                            <svg class="w-10 h-10 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"></path>
                            </svg>
                        </div>
                        <h3 class="text-lg font-bold text-slate-700">주문 내역이 없습니다.</h3>
                        <p class="text-slate-500 mt-2 text-sm">새로운 상품을 둘러보고 마음에 드는 상품을 주문해보세요.</p>
                        <a href="/" class="inline-block mt-6 px-6 py-2.5 bg-slate-900 text-white rounded-xl font-bold text-sm hover:bg-slate-800 transition-colors">쇼핑하러 가기</a>
                    </div>
                )}
            </div>

            {/* ========================================================================= */}
            {/* 1. 배송 조회 모달 */}
            {/* ========================================================================= */}
            <div id="trackModal" className={`modal-overlay ${isTrackOpen ? 'active' : ''}`} onClick={(e) => e.target.id === 'trackModal' && closeTrackModal()}>
                <div class="modal-content">
                    <h3 class="text-xl font-bold text-slate-900 mb-6">배송 상태 조회</h3>

                    {/* 배송상태 흐름도 */}
                    <div class="mb-8 px-2">
                        <div class="flex items-center justify-between relative">
                            <div class="absolute left-0 right-0 top-5 -translate-y-1/2 h-1 bg-slate-100 -z-10"></div>
                            <div id="stepProgress" class="absolute left-0 top-5 -translate-y-1/2 h-1 bg-blue-500 transition-all duration-500 -z-10" style={{ width: trackModalUI.stepProgressWidth }}></div>

                            {/* Step 1: 배송대기 */}
                            <div id="step1" class="flex flex-col items-center flex-1 relative z-10">
                                <div className={`step-circle w-10 h-10 rounded-full flex items-center justify-center border-2 font-bold transition-all duration-300 ${trackModalUI.step1Active ? 'bg-blue-500 border-blue-500 text-white shadow-md shadow-blue-100' : 'bg-slate-100 border-slate-200 text-slate-400'}`}>
                                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"></path></svg>
                                </div>
                                <span className={`step-label text-xs font-bold mt-2 transition-all ${trackModalUI.step1Active ? 'text-blue-600' : 'text-slate-400'}`}>배송대기</span>
                            </div>

                            {/* Step 2: 중간허브도착 */}
                            <div id="step2" class="flex flex-col items-center flex-1 relative z-10">
                                <div className={`step-circle w-10 h-10 rounded-full flex items-center justify-center border-2 font-bold transition-all duration-300 ${trackModalUI.step2Active ? 'bg-blue-500 border-blue-500 text-white shadow-md shadow-blue-100' : 'bg-slate-100 border-slate-200 text-slate-400'}`}>
                                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"></path></svg>
                                </div>
                                <span className={`step-label text-xs font-bold mt-2 transition-all ${trackModalUI.step2Active ? 'text-blue-600' : 'text-slate-400'}`}>중간허브도착</span>
                            </div>

                            {/* Step 3: 배송완료 */}
                            <div id="step3" class="flex flex-col items-center flex-1 relative z-10">
                                <div className={`step-circle w-10 h-10 rounded-full flex items-center justify-center border-2 font-bold transition-all duration-300 ${trackModalUI.step3Active ? 'bg-blue-500 border-blue-500 text-white shadow-md shadow-blue-100' : 'bg-slate-100 border-slate-200 text-slate-400'}`}>
                                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
                                </div>
                                <span className={`step-label text-xs font-bold mt-2 transition-all ${trackModalUI.step3Active ? 'text-blue-600' : 'text-slate-400'}`}>배송완료</span>
                            </div>
                        </div>
                    </div>

                    <p class="text-slate-500 text-sm mb-6">현재 배송 상태: <span id="modalStatusText" className={`font-bold ${trackModalUI.status === 'CANCELED' || trackModalUI.status === 'FAILED' ? 'text-red-600' : 'text-blue-600'}`}>{trackModalUI.status}</span></p>

                    <div class="bg-slate-50 p-4 rounded-xl border border-slate-200 mb-6">
                        <p class="text-xs font-bold text-slate-400 mb-1 whitespace-nowrap">
                            운송장 번호 <span id="modalCompanyName" class="font-semibold text-slate-500 ml-1">{trackModalUI.companyName}</span>
                        </p>
                        <p id="modalTrackingNumber" class="text-lg font-black tracking-widest text-slate-800 select-all">{trackModalUI.trackingNumber}</p>
                    </div>

                    <button onClick={closeTrackModal} class="w-full bg-slate-900 hover:bg-slate-800 text-white font-bold py-3.5 rounded-xl transition-colors text-sm shadow-md">확인</button>
                </div>
            </div>

            {/* ========================================================================= */}
            {/* 2. 경고 모달: 이미 배송된 상품 취소 시도 */}
            {/* ========================================================================= */}
            <div id="cancelAlertModal" className={`modal-overlay ${isCancelAlertOpen ? 'active' : ''}`} onClick={(e) => e.target.id === 'cancelAlertModal' && setIsCancelAlertOpen(false)}>
                <div class="modal-content">
                    <div class="w-16 h-16 bg-red-50 text-red-500 rounded-full flex items-center justify-center mx-auto mb-4 border border-red-100">
                        <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path>
                        </svg>
                    </div>
                    <h3 class="text-xl font-bold text-slate-900 mb-2">취소 불가 안내</h3>
                    <p class="text-slate-500 text-sm mb-6 leading-relaxed">이미 상품 배송이 시작되어<br/>주문을 취소할 수 없습니다.</p>
                    <button onClick={() => setIsCancelAlertOpen(false)} class="w-full bg-slate-900 hover:bg-slate-800 text-white font-bold py-3.5 rounded-xl transition-colors text-sm">확인</button>
                </div>
            </div>

            {/* ========================================================================= */}
            {/* 3. 경고 모달: 반품 아이템 미선택 시 */}
            {/* ========================================================================= */}
            <div id="returnNoSelectionModal" className={`modal-overlay ${isReturnNoSelectionOpen ? 'active' : ''}`} onClick={(e) => e.target.id === 'returnNoSelectionModal' && setIsReturnNoSelectionOpen(false)}>
                <div class="modal-content">
                    <div class="w-16 h-16 bg-amber-50 text-amber-500 rounded-full flex items-center justify-center mx-auto mb-4 border border-amber-100">
                        <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path>
                        </svg>
                    </div>
                    <h3 class="text-xl font-bold text-slate-900 mb-2">상품 미선택</h3>
                    <p class="text-slate-500 text-sm mb-6 leading-relaxed">반품 신청을 진행할 상품을<br/>최소 하나 이상 체크해 주세요.</p>
                    <button onClick={() => setIsReturnNoSelectionOpen(false)} class="w-full bg-slate-900 hover:bg-slate-800 text-white font-bold py-3.5 rounded-xl transition-colors text-sm">확인</button>
                </div>
            </div>

            {/* ========================================================================= */}
            {/* 4. 취소 / 반품 확인 및 결과 처리 통합 모달 */}
            {/* ========================================================================= */}
            <div id="confirmModal" className={`modal-overlay ${confirmModal.isOpen ? 'active' : ''}`}
                 onClick={(e) => {
                     if (e.target.id === 'confirmModal' && confirmModal.step !== 'PROCESSING') {
                         setConfirmModal(prev => ({ ...prev, isOpen: false }));
                     }
                 }}>
                <div class="modal-content">
                    {/* [Step A] 확인/요청 단계 */}
                    {confirmModal.step === 'CONFIRM' && (
                        <div id="confirmStep">
                            <div className={`w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4 border ${confirmModal.type === 'CANCEL' ? 'bg-red-50 border-red-100 text-red-500' : 'bg-amber-50 border-amber-100 text-amber-500'}`}>
                                <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                                </svg>
                            </div>
                            <h3 class="text-xl font-bold text-slate-900 mb-2">{confirmModal.title}</h3>
                            <p class="text-slate-500 text-sm mb-6 leading-relaxed" dangerouslySetInnerHTML={{ __html: confirmModal.text }}></p>
                            
                            <div class="flex gap-3">
                                <button onClick={() => setConfirmModal(prev => ({ ...prev, isOpen: false }))} class="flex-1 bg-slate-100 hover:bg-slate-200 text-slate-600 font-bold py-3.5 rounded-xl transition-colors text-sm">아니오</button>
                                <button onClick={confirmModal.type === 'CANCEL' ? executeOrderCancel : executeOrderReturn} 
                                        className={`flex-1 text-white font-bold py-3.5 rounded-xl transition-colors text-sm shadow-md ${confirmModal.type === 'CANCEL' ? 'bg-red-500 hover:bg-red-600 shadow-red-100' : 'bg-amber-500 hover:bg-amber-600 shadow-amber-100'}`}>예, 진행합니다</button>
                            </div>
                        </div>
                    )}

                    {/* [Step B] 처리 중 로딩 단계 */}
                    {confirmModal.step === 'PROCESSING' && (
                        <div id="processingStep" class="py-6">
                            <div class="w-12 h-12 border-4 border-slate-200 border-t-amber-500 rounded-full animate-spin mx-auto mb-4"></div>
                            <h3 class="text-lg font-bold text-slate-900 mb-1">요청을 처리하고 있습니다</h3>
                            <p class="text-slate-400 text-xs">잠시만 기다려 주세요...</p>
                        </div>
                    )}

                    {/* [Step C] 최종 성공/실패 결과 단계 */}
                    {confirmModal.step === 'RESULT' && (
                        <div id="resultStep">
                            <div className={`w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4 border ${confirmModal.isSuccess ? 'bg-emerald-50 border-emerald-100 text-emerald-500' : 'bg-red-50 border-red-100 text-red-500'}`}>
                                {confirmModal.isSuccess ? (
                                    <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
                                    </svg>
                                ) : (
                                    <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                                    </svg>
                                )}
                            </div>
                            <h3 class="text-xl font-bold text-slate-900 mb-2">{confirmModal.resultTitle}</h3>
                            <p class="text-slate-500 text-sm mb-6 leading-relaxed">{confirmModal.resultMessage}</p>
                            <button onClick={() => {
                                setConfirmModal(prev => ({ ...prev, isOpen: false }));
                                // 처리 완료 후 목록을 비동기식으로 실시간 갱신합니다.
                                if (confirmModal.isSuccess) {
                                    fetchOrders(keyword, period);
                                }
                            }} class="w-full bg-slate-900 hover:bg-slate-800 text-white font-bold py-3.5 rounded-xl transition-colors text-sm">확인</button>
                        </div>
                    )}
                </div>
            </div>
        </section>
    );
}

// React root mount
const el = document.getElementById('orders-root');
if (el) {
    createRoot(el).render(<App />);
}
