import React, { useState, useEffect, useRef } from 'react';
import { createRoot } from 'react-dom/client';

function App() {
    const [cancelReturns, setCancelReturns] = useState([]);
    const [loading, setLoading] = useState(true);

    const [hasMore, setHasMore] = useState(true);
    const pageSize = 5;

    // 상세 모달 상태
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [modalUI, setModalUI] = useState({
        type: 'CANCEL',
        title: '반품 상세 정보',
        labelRequestDate: '반품접수일자',
        labelUid: '반품접수번호',
        labelReasonHeader: '반품 사유',
        showDeliveryFee: true,
        thumbnailUrl: '',
        productName: '',
        productPrice: 0,
        quantity: 0,
        statusText: '',
        itemStatus: 0,
        requestDate: '',
        uid: '',
        completedDate: '',
        reason: '',
        originalPrice: 0,
        discountPrice: 0,
        deliveryFee: 0,
        refundPrice: 0,
        paymentMethod: ''
    });
    
    const resetTimeoutRef = useRef(null);

    // 무한스크롤 자동 활성화 여부
    const [isInfiniteScrollEnabled, setIsInfiniteScrollEnabled] = useState(false);

    const fetchCancelReturns = async (offset = 0, size = 5, isAppend = false) => {
        setLoading(true);
        try {
            const response = await fetch(`/api/mypage/returns?offset=${offset}&size=${size}`);
            if (!response.ok) throw new Error("데이터 수집 실패");
            const data = await response.json();
            
            if (isAppend) {
                setCancelReturns(prev => [...prev, ...data]);
            } else {
                setCancelReturns(data);
            }
            
            if (data.length < size) {
                setHasMore(false);
            } else {
                setHasMore(true);
            }
        } catch (error) {
            console.error("취소/반품 내역 로드 중 에러: ", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchCancelReturns(0, 5, false);
    }, []);

    // 무한스크롤 이벤트 리스너 (첫번째 더보기 클릭 이후부터는 자동 스크롤 로딩 활성화)
    useEffect(() => {
        const handleScroll = () => {
            if (loading || !hasMore || !isInfiniteScrollEnabled) return;

            const scrollHeight = document.documentElement.scrollHeight;
            const scrollTop = document.documentElement.scrollTop || document.body.scrollTop;
            const clientHeight = document.documentElement.clientHeight;

            if (scrollHeight - scrollTop - clientHeight < 200) {
                fetchCancelReturns(cancelReturns.length, 5, true);
            }
        };

        window.addEventListener('scroll', handleScroll);
        return () => window.removeEventListener('scroll', handleScroll);
    }, [loading, hasMore, isInfiniteScrollEnabled, cancelReturns.length]);

    const handleLoadMore = () => {
        setIsInfiniteScrollEnabled(true);
        fetchCancelReturns(cancelReturns.length, 5, true);
    };

    // 모달 열기
    const openDetailModal = (cr) => {
        if (resetTimeoutRef.current) {
            clearTimeout(resetTimeoutRef.current);
            resetTimeoutRef.current = null;
        }

        const isCancel = cr.type === 'CANCEL';
        
        setModalUI({
            type: cr.type,
            title: isCancel ? '취소 상세 정보' : '반품 상세 정보',
            labelRequestDate: isCancel ? '취소접수일자' : '반품접수일자',
            labelUid: isCancel ? '취소접수번호' : '반품접수번호',
            labelReasonHeader: isCancel ? '취소 사유' : '반품 사유',
            showDeliveryFee: !isCancel,
            thumbnailUrl: cr.thumbnailUrl || '/images/default-product.png',
            productName: cr.productName,
            productPrice: cr.productPrice || 0,
            quantity: cr.quantity || 0,
            statusText: cr.statusText,
            itemStatus: cr.itemStatus || 0,
            requestDate: cr.requestDate || '-',
            uid: cr.uid || '-',
            completedDate: cr.completedDate || '진행 중',
            reason: cr.reason || '-',
            originalPrice: cr.originalPrice || ((cr.productPrice || 0) * (cr.quantity || 0)),
            discountPrice: cr.discountPrice || 0,
            deliveryFee: cr.deliveryFee || 0,
            refundPrice: cr.refundPrice || 0,
            paymentMethod: cr.paymentMethod || '토스 페이먼츠'
        });
        setIsModalOpen(true);
    };

    // 모달 닫기 (150ms 딜레이)
    const closeDetailModal = () => {
        setIsModalOpen(false);

        if (resetTimeoutRef.current) {
            clearTimeout(resetTimeoutRef.current);
        }

        resetTimeoutRef.current = setTimeout(() => {
            setModalUI({
                type: 'CANCEL',
                title: '반품 상세 정보',
                labelRequestDate: '반품접수일자',
                labelUid: '반품접수번호',
                labelReasonHeader: '반품 사유',
                showDeliveryFee: true,
                thumbnailUrl: '',
                productName: '',
                productPrice: 0,
                quantity: 0,
                statusText: '',
                itemStatus: 0,
                requestDate: '',
                uid: '',
                completedDate: '',
                reason: '',
                originalPrice: 0,
                discountPrice: 0,
                deliveryFee: 0,
                refundPrice: 0,
                paymentMethod: ''
            });
            resetTimeoutRef.current = null;
        }, 150);
    };

    const formatWon = (num) => (num || 0).toLocaleString('ko-KR') + '원';

    return (
        <section className="space-y-6">
            <div className="flex justify-between items-end mb-8">
                <div>
                    <h1 className="text-2xl font-extrabold text-slate-900">취소/반품/교환/환불내역</h1>
                    <p className="text-slate-500 mt-2 text-sm">신청하신 주문 취소 및 반품 진행 내역을 확인할 수 있습니다.</p>
                </div>
            </div>

            {/* 취소/반품 카드 리스트 */}
            <div className="space-y-5">
                {cancelReturns && cancelReturns.length > 0 ? (
                    <>
                        {cancelReturns.map((cr, index) => {
                            const isCancel = cr.type === 'CANCEL';
                            let statusBadgeClass = 'status-badge font-extrabold ';
                            if (isCancel) {
                                statusBadgeClass += 'status-CANCEL';
                            } else if (cr.itemStatus === 9) {
                                statusBadgeClass += 'status-RETURNED';
                            } else {
                                statusBadgeClass += 'status-RETURN';
                            }

                            return (
                                <article key={cr.orderItemSeq || index} className="premium-card overflow-hidden">
                                    <div className="px-6 py-4 bg-slate-50/50 border-b border-slate-100 flex justify-between items-center flex-wrap gap-2 text-sm">
                                        <div className="flex items-center gap-3">
                                            <span className="text-slate-400 font-bold">
                                                {isCancel ? '주문취소 접수일' : '반품 접수일'}
                                            </span>
                                            <span className="font-bold text-slate-700">{cr.requestDate}</span>
                                        </div>
                                        <div className="flex items-center gap-3 text-slate-400">
                                            <span>주문번호:</span>
                                            <span className="font-semibold text-slate-600">{cr.orderUid}</span>
                                            {cr.deliveryCompany && (
                                                <span className="text-xs font-bold text-amber-600 bg-amber-50 border border-amber-100 px-2 py-0.5 rounded-full">
                                                    {cr.deliveryCompany}
                                                </span>
                                            )}
                                        </div>
                                    </div>

                                    <div className="p-6 flex flex-col lg:flex-row justify-between items-start lg:items-center gap-6">
                                        {/* 상품 목록 영역 (배송 묶음 전체 나열) */}
                                        <div className="flex-1 space-y-4 w-full">
                                            {cr.items && cr.items.length > 0 ? (
                                                cr.items.map((item, itemIdx) => (
                                                    <div key={itemIdx} className="flex gap-5 items-center pb-3 last:pb-0 border-b border-slate-100/50 last:border-b-0">
                                                        <div className="relative w-16 h-16 rounded-xl overflow-hidden shadow-sm border border-slate-100 shrink-0">
                                                            <img src={item.thumbnailUrl || '/images/default-product.png'} alt="상품 이미지" className={`w-full h-full object-cover ${[6,7,8,9].includes(item.itemStatus) ? ' grayscale' : ''}`} />
                                                            {item.itemStatus === 6 && (
                                                                <div className="absolute inset-0 bg-slate-900/45 flex items-center justify-center">
                                                                    <span className="text-[10px] font-black text-white border border-white/60 px-1 py-0.5 rounded">취소됨</span>
                                                                </div>
                                                            )}
                                                            {item.itemStatus === 7 && (
                                                                <div className="absolute inset-0 bg-slate-900/45 flex items-center justify-center">
                                                                    <span className="text-[10px] font-black text-white border border-white/60 px-1 py-0.5 rounded">반품대기</span>
                                                                </div>
                                                            )}
                                                            {item.itemStatus === 8 && (
                                                                <div className="absolute inset-0 bg-slate-900/45 flex items-center justify-center">
                                                                    <span className="text-[10px] font-black text-white border border-white/60 px-1 py-0.5 rounded">반품진행</span>
                                                                </div>
                                                            )}
                                                            {item.itemStatus === 9 && (
                                                                <div className="absolute inset-0 bg-slate-900/45 flex items-center justify-center">
                                                                    <span className="text-[10px] font-black text-white border border-white/60 px-1 py-0.5 rounded">반품완료</span>
                                                                </div>
                                                            )}
                                                        </div>
                                                        <div className="space-y-0.5">
                                                            <h4 className="font-bold text-sm text-slate-950 leading-tight">{item.productName}</h4>
                                                            <p className="text-xs text-slate-500 font-medium">
                                                                <span>{formatWon(item.productPrice)}</span>
                                                                <span className="mx-1">/</span>
                                                                <span>{item.quantity}개</span>
                                                            </p>
                                                        </div>
                                                    </div>
                                                ))
                                            ) : (
                                                <div className="flex gap-5 items-center">
                                                    <div className="relative w-16 h-16 rounded-xl overflow-hidden shadow-sm border border-slate-100 shrink-0">
                                                        <img src={cr.thumbnailUrl || '/images/default-product.png'} alt="상품 이미지" className={`w-full h-full object-cover ${[6,7,8,9].includes(cr.itemStatus) ? ' grayscale' : ''}`} />
                                                        {cr.itemStatus === 6 && (
                                                            <div className="absolute inset-0 bg-slate-900/45 flex items-center justify-center">
                                                                <span className="text-[10px] font-black text-white border border-white/60 px-1 py-0.5 rounded">취소됨</span>
                                                            </div>
                                                        )}
                                                        {cr.itemStatus === 7 && (
                                                            <div className="absolute inset-0 bg-slate-900/45 flex items-center justify-center">
                                                                <span className="text-[10px] font-black text-white border border-white/60 px-1 py-0.5 rounded">반품대기</span>
                                                            </div>
                                                        )}
                                                        {cr.itemStatus === 8 && (
                                                            <div className="absolute inset-0 bg-slate-900/45 flex items-center justify-center">
                                                                <span className="text-[10px] font-black text-white border border-white/60 px-1 py-0.5 rounded">반품진행</span>
                                                            </div>
                                                        )}
                                                        {cr.itemStatus === 9 && (
                                                            <div className="absolute inset-0 bg-slate-900/45 flex items-center justify-center">
                                                                <span className="text-[10px] font-black text-white border border-white/60 px-1 py-0.5 rounded">반품완료</span>
                                                            </div>
                                                        )}
                                                    </div>
                                                    <div className="space-y-0.5">
                                                        <h4 className="font-bold text-sm text-slate-950 leading-tight">{cr.productName}</h4>
                                                        <p className="text-xs text-slate-500 font-medium">
                                                            <span>{formatWon(cr.productPrice)}</span>
                                                            <span className="mx-1">/</span>
                                                            <span>{cr.quantity}개</span>
                                                        </p>
                                                    </div>
                                                </div>
                                            )}
                                        </div>

                                        {/* 상태 및 상세조회 버튼 */}
                                        <div className="w-full lg:w-auto flex justify-between lg:justify-end items-center gap-6 self-stretch lg:self-auto border-t lg:border-t-0 pt-4 lg:pt-0 border-slate-100">
                                            <div className="flex flex-col items-start lg:items-center gap-1">
                                                <span className="text-xs text-slate-400 font-bold">진행 상태</span>
                                                <span className={statusBadgeClass}>{cr.statusText}</span>
                                            </div>

                                            <button type="button"
                                                    onClick={() => openDetailModal(cr)}
                                                    className="btn-detail py-2.5 px-6 border text-slate-700 bg-white border-slate-200 hover:border-amber-500 hover:text-amber-600 text-sm shadow-sm shrink-0">
                                                <span>{isCancel ? '취소 상세' : '반품 상세'}</span>
                                            </button>
                                        </div>
                                    </div>
                                </article>
                            );
                        })}
                        {hasMore && isInfiniteScrollEnabled && loading && (
                            <div className="text-center pt-4 text-slate-500 font-bold text-sm">
                                로딩 중...
                            </div>
                        )}
                        {hasMore && !isInfiniteScrollEnabled && (
                            <div className="text-center pt-4">
                                <button type="button"
                                        disabled={loading}
                                        onClick={handleLoadMore}
                                        className="px-8 py-3 bg-white border border-slate-200 text-slate-700 rounded-xl font-bold text-sm hover:bg-slate-50 transition-colors shadow-sm inline-flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed">
                                    {loading ? '로딩 중...' : '더보기'} <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7"></path></svg>
                                </button>
                            </div>
                        )}
                    </>
                ) : loading ? (
                    <div className="bg-white p-12 text-center border border-slate-200 rounded-2xl shadow-sm">
                        <p className="text-slate-500 text-sm">정보를 불러오는 중입니다...</p>
                    </div>
                ) : (
                    <div className="premium-card p-12 text-center bg-white">
                        <div className="w-20 h-20 bg-slate-100 rounded-full flex items-center justify-center mx-auto mb-4">
                            <svg className="w-10 h-10 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01"></path>
                            </svg>
                        </div>
                        <h3 className="text-lg font-bold text-slate-700">취소/반품 내역이 없습니다.</h3>
                        <p className="text-slate-500 mt-2 text-sm">신청 완료된 취소 및 반품 내역이 이곳에 표시됩니다.</p>
                    </div>
                )}
            </div>

            {/* 취소/반품 상세 통합 모달 */}
            <div id="detailModal" 
                 className={`modal-overlay ${isModalOpen ? 'active' : ''}`} 
                 onClick={(e) => e.target.id === 'detailModal' && closeDetailModal()}>
                
                <div className="modal-content text-left space-y-6 relative max-h-[90vh] overflow-y-auto">
                    {/* Close Button */}
                    <button onClick={closeDetailModal} className="absolute top-4 right-4 text-slate-400 hover:text-slate-600">
                        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                        </svg>
                    </button>

                    <h3 className="text-xl font-extrabold text-slate-900 border-b border-slate-100 pb-3">{modalUI.title}</h3>

                    {/* 1. 상품 정보 카드 */}
                    <div className="flex gap-4 items-center bg-slate-50 p-4 rounded-2xl border border-slate-100">
                        <div className="relative w-16 h-16 rounded-xl overflow-hidden shadow-sm shrink-0 border border-slate-200">
                            <img src={modalUI.thumbnailUrl} alt="상품 이미지" className={`w-full h-full object-cover ${[6,7,8,9].includes(modalUI.itemStatus) ? ' grayscale' : ''}`} />
                            {modalUI.itemStatus === 6 && (
                                <div className="absolute inset-0 bg-slate-900/45 flex items-center justify-center">
                                    <span className="text-[10px] font-black text-white border border-white/60 px-1 py-0.5 rounded">취소됨</span>
                                </div>
                            )}
                            {modalUI.itemStatus === 7 && (
                                <div className="absolute inset-0 bg-slate-900/45 flex items-center justify-center">
                                    <span className="text-[10px] font-black text-white border border-white/60 px-1 py-0.5 rounded">반품대기</span>
                                </div>
                            )}
                            {modalUI.itemStatus === 8 && (
                                <div className="absolute inset-0 bg-slate-900/45 flex items-center justify-center">
                                    <span className="text-[10px] font-black text-white border border-white/60 px-1 py-0.5 rounded">반품진행</span>
                                </div>
                            )}
                            {modalUI.itemStatus === 9 && (
                                <div className="absolute inset-0 bg-slate-900/45 flex items-center justify-center">
                                    <span className="text-[10px] font-black text-white border border-white/60 px-1 py-0.5 rounded">반품완료</span>
                                </div>
                            )}
                        </div>
                        <div className="space-y-0.5">
                            <h4 className="font-bold text-sm text-slate-900 leading-tight">{modalUI.productName}</h4>
                            <p className="text-xs text-slate-500 font-medium">
                                <span>{formatWon(modalUI.productPrice)}</span> / <span>{modalUI.quantity}개</span>
                            </p>
                            <div className="pt-1">
                                <span className={`status-badge text-[10px] font-black ${
                                    modalUI.type === 'CANCEL' 
                                        ? 'status-CANCEL' 
                                        : (modalUI.statusText === '반품완료' ? 'status-RETURNED' : 'status-RETURN')
                                }`}>
                                    {modalUI.statusText}
                                </span>
                            </div>
                        </div>
                    </div>

                    {/* 2. 접수 상세 정보 */}
                    <div className="space-y-3">
                        <h4 className="font-bold text-sm text-slate-900 flex items-center gap-1.5">
                            <span className="w-1 h-3 bg-amber-500 rounded-full"></span>접수 정보
                        </h4>
                        <div className="bg-slate-50 p-4 rounded-2xl border border-slate-100 text-xs space-y-2.5 text-slate-600 font-medium">
                            <div className="flex justify-between">
                                <span>{modalUI.labelRequestDate}</span>
                                <span className="text-slate-900 font-bold">{modalUI.requestDate}</span>
                            </div>
                            <div className="flex justify-between">
                                <span>{modalUI.labelUid}</span>
                                <span className="text-slate-900 font-bold tracking-wider">{modalUI.uid}</span>
                            </div>
                            <div className="flex justify-between">
                                <span>완료일자</span>
                                <span className="text-slate-900 font-bold">{modalUI.completedDate}</span>
                            </div>
                        </div>
                    </div>

                    {/* 3. 사유 정보 */}
                    <div className="space-y-3">
                        <h4 className="font-bold text-sm text-slate-900 flex items-center gap-1.5">
                            <span className="w-1 h-3 bg-amber-500 rounded-full"></span>{modalUI.labelReasonHeader}
                        </h4>
                        <div className="bg-slate-50 p-4 rounded-2xl border border-slate-100 text-xs text-slate-800 font-bold">
                            {modalUI.reason}
                        </div>
                    </div>

                    {/* 4. 환불 정산 안내 */}
                    <div className="space-y-3">
                        <h4 className="font-bold text-sm text-slate-900 flex items-center gap-1.5">
                            <span className="w-1 h-3 bg-amber-500 rounded-full"></span>환불 안내
                        </h4>
                        <div className="bg-slate-50 p-4 rounded-2xl border border-slate-100 text-xs space-y-3 text-slate-600 font-medium">
                            <div className="flex justify-between">
                                <span>상품 금액</span>
                                <span className="text-slate-900 font-bold">{formatWon(modalUI.originalPrice)}</span>
                            </div>
                            <div className="flex justify-between">
                                <span>할인 금액</span>
                                <span className="text-red-500 font-bold">-{formatWon(modalUI.discountPrice)}</span>
                            </div>
                            {modalUI.showDeliveryFee && (
                                <div className="flex justify-between">
                                    <span>반품 배송비</span>
                                    <span className="text-slate-900 font-bold">-{formatWon(modalUI.deliveryFee)}</span>
                                </div>
                            )}
                            <div className="border-t border-slate-200/60 pt-3 flex justify-between items-center text-sm font-extrabold">
                                <span className="text-slate-900">최종 환불 금액</span>
                                <span className="text-amber-600 font-black text-base">{formatWon(modalUI.refundPrice)}</span>
                            </div>
                            <div className="border-t border-slate-200/60 pt-3 flex justify-between">
                                <span>환불 수단</span>
                                <span className="text-slate-900 font-bold">{modalUI.paymentMethod}</span>
                            </div>
                            <div className="flex justify-between">
                                <span>환불 상태</span>
                                <span className="text-emerald-600 font-bold">환불 완료</span>
                            </div>
                        </div>
                    </div>

                    <button onClick={closeDetailModal} className="w-full bg-slate-900 hover:bg-slate-800 text-white font-bold py-3 rounded-xl transition-colors text-sm">확인</button>
                </div>
            </div>
        </section>
    );
}

// React root mount
const el = document.getElementById('returns-root');
if (el) {
    createRoot(el).render(<App />);
}
