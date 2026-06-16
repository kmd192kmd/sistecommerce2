import { i as e, n as t, r as n, t as r } from "./jsx-runtime-DGhihkRN.js";
//#region src/mypage-orders.jsx
var i = /* @__PURE__ */ e(n(), 1), a = t(), o = r();
function s() {
	let [e, t] = (0, i.useState)([]), [n, r] = (0, i.useState)(!0), [a, s] = (0, i.useState)(() => new URLSearchParams(window.location.search).get("keyword") || ""), [c, l] = (0, i.useState)(() => new URLSearchParams(window.location.search).get("period") || "6months"), [u, d] = (0, i.useState)(!0), [f, p] = (0, i.useState)(!1), [m, h] = (0, i.useState)({
		trackingNumber: "",
		status: "READY",
		companyName: ""
	}), [g, _] = (0, i.useState)({
		trackingNumber: "-",
		status: "READY",
		companyName: "",
		stepProgressWidth: "0%",
		step1Active: !1,
		step2Active: !1,
		step3Active: !1
	}), v = (0, i.useRef)(null), [y, b] = (0, i.useState)(!1), [x, S] = (0, i.useState)("이미 상품 배송이 시작되어 주문을 취소할 수 없습니다."), [C, w] = (0, i.useState)(!1), [T, E] = (0, i.useState)(!1), [D, O] = (0, i.useState)({
		isOpen: !1,
		type: "",
		title: "",
		text: "",
		step: "CONFIRM",
		isSuccess: !1,
		resultTitle: "",
		resultMessage: "",
		payload: {}
	}), [k, A] = (0, i.useState)({}), [j, M] = (0, i.useState)(!1), [N, P] = (0, i.useState)(""), F = async (e, n, i = 0, a = 5, o = !1) => {
		r(!0);
		try {
			let r = new URLSearchParams();
			e && r.append("keyword", e), n && r.append("period", n), r.append("offset", i.toString()), r.append("size", a.toString());
			let s = await fetch(`/api/mypage/orders?${r.toString()}`);
			if (!s.ok) throw Error("데이터 수집 실패");
			let c = await s.json();
			t(o ? (e) => [...e, ...c] : c), c.length < a ? d(!1) : d(!0);
		} catch (e) {
			console.error("주문목록 로드 중 에러: ", e);
		} finally {
			r(!1);
		}
	};
	(0, i.useEffect)(() => {
		F(a, c, 0, 5, !1);
	}, []), (0, i.useEffect)(() => {
		let t = () => {
			if (n || !u || !j) return;
			let t = document.documentElement.scrollHeight, r = document.documentElement.scrollTop || document.body.scrollTop, i = document.documentElement.clientHeight;
			t - r - i < 200 && F(a, c, e.length, 5, !0);
		};
		return window.addEventListener("scroll", t), () => window.removeEventListener("scroll", t);
	}, [
		n,
		u,
		j,
		e.length,
		a,
		c
	]);
	let I = (e) => {
		e.preventDefault();
		let t = new URLSearchParams(window.location.search);
		a ? t.set("keyword", a) : t.delete("keyword"), t.set("period", c), window.history.replaceState({}, "", `${window.location.pathname}?${t.toString()}`), M(!1), F(a, c, 0, 5, !1);
	}, L = (e) => {
		l(e);
		let t = new URLSearchParams(window.location.search);
		t.set("period", e), a ? t.set("keyword", a) : t.delete("keyword"), window.history.replaceState({}, "", `${window.location.pathname}?${t.toString()}`), M(!1), F(a, e, 0, 5, !1);
	}, R = () => {
		M(!0), F(a, c, e.length, 5, !0);
	}, z = (e, t, n) => {
		v.current &&= (clearTimeout(v.current), null);
		let r = t || "READY", i = "0%", a = !1, o = !1, s = !1;
		r !== "CANCELED" && r !== "FAILED" && (r === "READY" ? (a = !0, i = "0%") : r === "SHIPPING" || r === "DELAYED" ? (a = !0, o = !0, i = "50%") : r === "DELIVERED" && (a = !0, o = !0, s = !0, i = "100%")), h({
			trackingNumber: e,
			status: r,
			companyName: n
		}), _({
			trackingNumber: e || "발급대기",
			status: r,
			companyName: n ? `(${n})` : "",
			stepProgressWidth: i,
			step1Active: a,
			step2Active: o,
			step3Active: s
		}), p(!0);
	}, B = () => {
		p(!1), v.current && clearTimeout(v.current), v.current = setTimeout(() => {
			_({
				trackingNumber: "-",
				status: "READY",
				companyName: "",
				stepProgressWidth: "0%",
				step1Active: !1,
				step2Active: !1,
				step3Active: !1
			}), v.current = null;
		}, 150);
	}, V = (e, t) => {
		A((n) => ({
			...n,
			[e]: t
		}));
	}, H = async () => {
		O((e) => ({
			...e,
			step: "PROCESSING"
		}));
		let { orderSeq: e, orderItemSeqList: t } = D.payload;
		try {
			let n = await fetch(`/api/mypage/orders/${e}/cancel-items`, {
				method: "POST",
				headers: {
					"Content-Type": "application/json",
					Accept: "application/json"
				},
				body: JSON.stringify({
					orderItemSeqList: t,
					cancelReason: N || "고객 주문 취소 (택배사 묶음 전체)"
				})
			}), r = await n.json();
			if (!n.ok) {
				O((e) => ({
					...e,
					step: "RESULT",
					isSuccess: !1,
					resultTitle: "취소 실패",
					resultMessage: r?.message || "주문 취소에 실패했습니다."
				}));
				return;
			}
			O((e) => ({
				...e,
				step: "RESULT",
				isSuccess: !0,
				resultTitle: "취소 완료",
				resultMessage: r?.message || "주문 취소가 완료되었습니다."
			}));
		} catch (e) {
			console.error(e), O((e) => ({
				...e,
				step: "RESULT",
				isSuccess: !1,
				resultTitle: "오류",
				resultMessage: "주문 취소 요청 중 오류가 발생했습니다."
			}));
		}
	}, U = async () => {
		O((e) => ({
			...e,
			step: "PROCESSING"
		}));
		let { orderSeq: e, orderItemSeqList: t } = D.payload;
		try {
			let n = await fetch(`/api/mypage/orders/${e}/return`, {
				method: "POST",
				headers: {
					"Content-Type": "application/json",
					Accept: "application/json"
				},
				body: JSON.stringify({
					orderItemSeqList: t,
					cancelReason: N || "고객 반품 신청"
				})
			}), r = await n.json();
			if (!n.ok) {
				O((e) => ({
					...e,
					step: "RESULT",
					isSuccess: !1,
					resultTitle: "반품 실패",
					resultMessage: r?.message || "반품 신청에 실패했습니다."
				}));
				return;
			}
			O((e) => ({
				...e,
				step: "RESULT",
				isSuccess: !0,
				resultTitle: "반품 완료",
				resultMessage: r?.message || "반품 신청이 접수되었습니다."
			}));
		} catch (e) {
			console.error(e), O((e) => ({
				...e,
				step: "RESULT",
				isSuccess: !1,
				resultTitle: "오류",
				resultMessage: "반품 신청 요청 중 오류가 발생했습니다."
			}));
		}
	}, W = (e, t) => {
		if (t.deliveryStatus !== "READY") {
			t.deliveryStatus === "DELIVERED" ? S("배송완료된 상품은 주문취소가 불가능합니다.") : t.deliveryStatus === "FAILED" ? S("배송 실패한 상품은 주문취소가 불가능합니다. 고객센터로 문의해 주세요.") : t.deliveryStatus === "DELAYED" ? S("배송 지연 중인 상품은 배송이 이미 진행되어 주문취소가 불가능합니다.") : S("이미 상품 배송이 시작되어 주문을 취소할 수 없습니다."), b(!0);
			return;
		}
		let n = t.items.filter((e) => e.itemStatus !== 6), r = n.map((e) => e.orderItemSeq);
		if (r.length === 0) return;
		let i = n[0].name, a = r.length > 1 ? `[${i} 외 ${r.length - 1}건]` : `[${i}]`;
		P("단순 변심"), O({
			isOpen: !0,
			type: "CANCEL",
			title: "주문 취소 확인",
			text: `${a}을 정말 주문 취소하시겠습니까?<br/><br/><span class="text-xs text-red-500 font-bold">※ 배송 준비 중이므로 즉시 환불 처리됩니다.</span>`,
			step: "CONFIRM",
			isSuccess: !1,
			resultTitle: "",
			resultMessage: "",
			payload: {
				orderSeq: e,
				orderItemSeqList: r
			}
		});
	}, G = (e, t) => {
		if (t.deliveryStatus !== "DELIVERED") {
			E(!0);
			return;
		}
		let n = t.items.map((e) => e.orderItemSeq).filter((e) => k[e] === !0);
		if (n.length === 0) {
			w(!0);
			return;
		}
		let r = t.items.filter((e) => n.includes(e.orderItemSeq))[0].name, i = n.length > 1 ? `[${r} 외 ${n.length - 1}건]` : `[${r}]`;
		P("단순 변심 (반품)"), O({
			isOpen: !0,
			type: "RETURN",
			title: "반품 신청 확인",
			text: `${i}을 정말 반품 신청하시겠습니까?<br/><br/><span class="text-xs text-amber-500 font-bold">※ 반품은 배송 완료 후 7일 이내에만 신청이 가능합니다.</span><br/><span class="text-xs text-amber-500 font-bold">※ 반품 신청 완료 후 택배 수거 및 검수가 진행됩니다.</span>`,
			step: "CONFIRM",
			isSuccess: !1,
			resultTitle: "",
			resultMessage: "",
			payload: {
				orderSeq: e,
				orderItemSeqList: n
			}
		});
	};
	return /* @__PURE__ */ (0, o.jsxs)("section", {
		class: "space-y-6",
		children: [
			/* @__PURE__ */ (0, o.jsx)("div", {
				class: "flex justify-between items-end mb-8",
				children: /* @__PURE__ */ (0, o.jsxs)("div", { children: [/* @__PURE__ */ (0, o.jsx)("h1", {
					class: "text-2xl font-extrabold text-slate-900",
					children: "주문목록 / 배송조회"
				}), /* @__PURE__ */ (0, o.jsx)("p", {
					class: "text-slate-500 mt-2 text-sm",
					children: "하나의 주문 안에서 택배사별 상품 묶음을 확인할 수 있습니다."
				})] })
			}),
			/* @__PURE__ */ (0, o.jsxs)("div", {
				class: "bg-white rounded-2xl p-6 shadow-sm border border-slate-200",
				children: [/* @__PURE__ */ (0, o.jsxs)("form", {
					onSubmit: I,
					class: "flex items-center gap-3",
					children: [/* @__PURE__ */ (0, o.jsxs)("div", {
						class: "relative flex-1",
						children: [/* @__PURE__ */ (0, o.jsx)("svg", {
							class: "w-5 h-5 absolute left-4 top-1/2 -translate-y-1/2 text-slate-400",
							fill: "none",
							stroke: "currentColor",
							viewBox: "0 0 24 24",
							children: /* @__PURE__ */ (0, o.jsx)("path", {
								"stroke-linecap": "round",
								"stroke-linejoin": "round",
								"stroke-width": "2",
								d: "M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
							})
						}), /* @__PURE__ */ (0, o.jsx)("input", {
							type: "text",
							value: a,
							onChange: (e) => s(e.target.value),
							placeholder: "주문하신 상품명을 검색해보세요",
							class: "w-full pl-12 pr-4 py-3 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-amber-500 focus:bg-white transition-all"
						})]
					}), /* @__PURE__ */ (0, o.jsx)("button", {
						type: "submit",
						class: "bg-amber-500 hover:bg-amber-600 text-white px-8 py-3 rounded-xl text-sm font-bold transition-colors shadow-sm shadow-amber-200",
						children: "검색"
					})]
				}), /* @__PURE__ */ (0, o.jsxs)("div", {
					class: "flex flex-wrap gap-2 text-xs font-semibold mt-4",
					children: [/* @__PURE__ */ (0, o.jsx)("button", {
						onClick: () => L("6months"),
						className: `px-4 py-2 rounded-lg transition-all ${c === "6months" ? "bg-slate-800 text-white" : "bg-slate-100 text-slate-600 hover:bg-slate-200"}`,
						children: "최근 6개월"
					}), [
						2026,
						2025,
						2024
					].map((e) => /* @__PURE__ */ (0, o.jsx)("button", {
						onClick: () => L(String(e)),
						className: `px-4 py-2 rounded-lg transition-all ${c === String(e) ? "bg-slate-800 text-white" : "bg-slate-100 text-slate-600 hover:bg-slate-200"}`,
						children: e
					}, e))]
				})]
			}),
			/* @__PURE__ */ (0, o.jsx)("div", {
				id: "orderListContainer",
				children: e && e.length > 0 ? /* @__PURE__ */ (0, o.jsxs)("div", {
					class: "space-y-5",
					children: [
						e.map((e) => /* @__PURE__ */ (0, o.jsxs)("article", {
							class: "premium-card overflow-hidden",
							children: [/* @__PURE__ */ (0, o.jsxs)("div", {
								class: "px-6 py-4 bg-slate-50/50 border-b border-slate-100 flex justify-between items-center",
								children: [/* @__PURE__ */ (0, o.jsxs)("div", {
									class: "text-sm flex items-center gap-3",
									children: [/* @__PURE__ */ (0, o.jsx)("span", {
										class: "text-slate-500 font-medium",
										children: "주문일자"
									}), /* @__PURE__ */ (0, o.jsx)("span", {
										class: "font-bold text-slate-800",
										children: e.orderDate
									})]
								}), /* @__PURE__ */ (0, o.jsx)("a", {
									href: `/mypage/orders/${e.orderSeq}`,
									class: "text-xs font-bold text-amber-600 hover:text-amber-700",
									children: "주문 상세보기"
								})]
							}), /* @__PURE__ */ (0, o.jsx)("div", {
								class: "bg-slate-50/60 px-3 py-4 sm:px-6 sm:py-5 space-y-4",
								children: e.deliveries.map((t, n) => {
									let r = t.items.filter((e) => ![
										6,
										7,
										8,
										9
									].includes(e.itemStatus)).length > 0, i = t.items.filter((e) => e.itemStatus !== 6).length > 0;
									return /* @__PURE__ */ (0, o.jsxs)("div", {
										class: "delivery-company-group p-5 sm:p-6",
										"data-order-seq": e.orderSeq,
										"data-company-name": t.companyName,
										children: [/* @__PURE__ */ (0, o.jsx)("div", {
											class: "mb-5 flex flex-col gap-2 border-b border-slate-100 pb-4 sm:flex-row sm:items-center sm:justify-between",
											children: /* @__PURE__ */ (0, o.jsxs)("div", {
												class: "flex flex-wrap items-center gap-2",
												children: [
													t.deliveryStatus === "DELIVERED" && /* @__PURE__ */ (0, o.jsxs)(o.Fragment, { children: [/* @__PURE__ */ (0, o.jsx)("span", {
														class: "status-badge status-DELIVERED",
														children: "배송완료"
													}), t.completedAt && /* @__PURE__ */ (0, o.jsx)("span", {
														class: "text-xs font-semibold text-slate-500",
														children: `도착일 ${t.completedAt}`
													})] }),
													t.deliveryStatus === "CANCELED" && /* @__PURE__ */ (0, o.jsx)("span", {
														class: "status-badge status-CANCELED",
														children: "주문취소"
													}),
													t.deliveryStatus === "SHIPPING" && /* @__PURE__ */ (0, o.jsx)("span", {
														class: "status-badge status-SHIPPING",
														children: "배송중"
													}),
													t.deliveryStatus === "DELAYED" && /* @__PURE__ */ (0, o.jsx)("span", {
														class: "status-badge status-DELAYED",
														children: "배송지연"
													}),
													t.deliveryStatus === "FAILED" && /* @__PURE__ */ (0, o.jsx)("span", {
														class: "status-badge status-FAILED",
														children: "배송실패"
													}),
													t.deliveryStatus === "READY" && /* @__PURE__ */ (0, o.jsx)("span", {
														class: "status-badge status-READY",
														children: "배송대기"
													})
												]
											})
										}), /* @__PURE__ */ (0, o.jsxs)("div", {
											class: "flex flex-col lg:flex-row gap-8",
											children: [/* @__PURE__ */ (0, o.jsx)("div", {
												class: "flex-1 space-y-4",
												children: t.items.map((e) => /* @__PURE__ */ (0, o.jsxs)("div", {
													class: `cancel-item-target flex gap-5 items-center pb-4 last:pb-0 last:border-b-0 border-b border-slate-100 ${[
														6,
														7,
														8,
														9
													].includes(e.itemStatus) ? " opacity-60" : ""}`,
													"data-order-item-seq": e.orderItemSeq,
													"data-item-status": e.itemStatus,
													children: [
														t.deliveryStatus === "DELIVERED" && ![
															6,
															7,
															8,
															9
														].includes(e.itemStatus) && /* @__PURE__ */ (0, o.jsx)("input", {
															type: "checkbox",
															checked: !!k[e.orderItemSeq],
															onChange: (t) => V(e.orderItemSeq, t.target.checked),
															class: "return-item-check w-5 h-5 accent-amber-500 shrink-0"
														}),
														/* @__PURE__ */ (0, o.jsx)("a", {
															href: `/products/${e.productSeq}`,
															class: "shrink-0",
															children: /* @__PURE__ */ (0, o.jsxs)("div", {
																class: "relative w-20 h-20 rounded-xl overflow-hidden shadow-sm border border-slate-100 hover:opacity-90 transition-opacity",
																children: [
																	/* @__PURE__ */ (0, o.jsx)("img", {
																		src: e.image,
																		alt: "상품 이미지",
																		class: `w-full h-full object-cover ${[
																			6,
																			7,
																			8,
																			9
																		].includes(e.itemStatus) ? " grayscale" : ""}`
																	}),
																	e.itemStatus === 6 && /* @__PURE__ */ (0, o.jsx)("div", {
																		class: "absolute inset-0 bg-slate-900/45 flex items-center justify-center",
																		children: /* @__PURE__ */ (0, o.jsx)("span", {
																			class: "text-[11px] font-black text-white border border-white/60 px-2 py-1 rounded",
																			children: "취소됨"
																		})
																	}),
																	e.itemStatus === 7 && /* @__PURE__ */ (0, o.jsx)("div", {
																		class: "absolute inset-0 bg-slate-900/45 flex items-center justify-center",
																		children: /* @__PURE__ */ (0, o.jsx)("span", {
																			class: "text-[11px] font-black text-white border border-white/60 px-2 py-1 rounded",
																			children: "반품대기"
																		})
																	}),
																	e.itemStatus === 8 && /* @__PURE__ */ (0, o.jsx)("div", {
																		class: "absolute inset-0 bg-slate-900/45 flex items-center justify-center",
																		children: /* @__PURE__ */ (0, o.jsx)("span", {
																			class: "text-[11px] font-black text-white border border-white/60 px-2 py-1 rounded",
																			children: "반품진행"
																		})
																	}),
																	e.itemStatus === 9 && /* @__PURE__ */ (0, o.jsx)("div", {
																		class: "absolute inset-0 bg-slate-900/45 flex items-center justify-center",
																		children: /* @__PURE__ */ (0, o.jsx)("span", {
																			class: "text-[11px] font-black text-white border border-white/60 px-2 py-1 rounded",
																			children: "반품완료"
																		})
																	})
																]
															})
														}),
														/* @__PURE__ */ (0, o.jsxs)("div", {
															class: "flex flex-col justify-center",
															children: [
																/* @__PURE__ */ (0, o.jsxs)("div", {
																	class: "flex items-center gap-2",
																	children: [
																		/* @__PURE__ */ (0, o.jsx)("a", {
																			href: `/products/${e.productSeq}`,
																			children: /* @__PURE__ */ (0, o.jsx)("h4", {
																				class: `font-bold text-base leading-tight transition-colors ${[
																					6,
																					7,
																					8,
																					9
																				].includes(e.itemStatus) ? " text-slate-400 line-through" : " text-slate-900 hover:text-amber-600"}`,
																				children: e.name
																			})
																		}),
																		e.itemStatus === 7 && /* @__PURE__ */ (0, o.jsx)("span", {
																			class: "text-[11px] font-bold bg-amber-50 text-amber-600 border border-amber-100 px-2 py-0.5 rounded-full",
																			children: "반품요청"
																		}),
																		e.itemStatus === 8 && /* @__PURE__ */ (0, o.jsx)("span", {
																			class: "text-[11px] font-bold bg-amber-50 text-amber-600 border border-amber-100 px-2 py-0.5 rounded-full",
																			children: "반품진행중"
																		}),
																		e.itemStatus === 9 && /* @__PURE__ */ (0, o.jsx)("span", {
																			class: "text-[11px] font-bold bg-slate-100 text-slate-600 border border-slate-200 px-2 py-0.5 rounded-full",
																			children: "반품완료"
																		})
																	]
																}),
																/* @__PURE__ */ (0, o.jsxs)("div", {
																	class: "mt-1.5 text-sm text-slate-500 font-medium",
																	children: [
																		/* @__PURE__ */ (0, o.jsx)("span", {
																			class: `font-bold ${[
																				6,
																				7,
																				8,
																				9
																			].includes(e.itemStatus) ? " text-slate-400 line-through" : " text-slate-700"}`,
																			children: e.price ? `${e.price.toLocaleString()}원` : "0원"
																		}),
																		/* @__PURE__ */ (0, o.jsx)("span", {
																			class: "mx-1",
																			children: "x"
																		}),
																		/* @__PURE__ */ (0, o.jsxs)("span", { children: [e.qty, "개"] })
																	]
																}),
																e.itemStatus === 6 && /* @__PURE__ */ (0, o.jsx)("p", {
																	class: "text-xs text-red-500 font-semibold mt-1",
																	children: "주문 취소가 완료된 상품입니다."
																}),
																e.itemStatus === 7 && /* @__PURE__ */ (0, o.jsx)("p", {
																	class: "text-xs text-amber-600 font-semibold mt-1",
																	children: "반품 신청이 접수되어 상품 수거 대기 중입니다."
																}),
																e.itemStatus === 8 && /* @__PURE__ */ (0, o.jsx)("p", {
																	class: "text-xs text-amber-600 font-semibold mt-1",
																	children: "상품 수거 완료 후 검수가 진행 중입니다."
																}),
																e.itemStatus === 9 && /* @__PURE__ */ (0, o.jsx)("p", {
																	class: "text-xs text-slate-500 font-semibold mt-1",
																	children: "반품 및 환불 처리가 완료되었습니다."
																}),
																e.itemStatus === 0 && t.deliveryStatus !== "READY" && /* @__PURE__ */ (0, o.jsx)("p", {
																	class: "text-xs text-slate-400 mt-1",
																	children: "배송대기 상태에서만 취소할 수 있습니다."
																})
															]
														})
													]
												}, e.orderItemSeq))
											}), /* @__PURE__ */ (0, o.jsx)("div", {
												class: "w-full lg:w-72 shrink-0 flex flex-col justify-center pl-0 lg:pl-6 border-t lg:border-t-0 lg:border-l border-slate-100 pt-5 lg:pt-0",
												children: /* @__PURE__ */ (0, o.jsxs)("div", {
													class: "flex flex-col gap-2 w-full",
													children: [
														/* @__PURE__ */ (0, o.jsx)("button", {
															type: "button",
															onClick: () => z(t.trackingNumber, t.deliveryStatus, t.companyName),
															class: "btn-action py-2.5 border border-slate-200 text-slate-700 bg-white hover:border-amber-500 hover:text-amber-600 text-sm shadow-sm",
															children: "배송조회"
														}),
														r ? /* @__PURE__ */ (0, o.jsx)("button", {
															type: "button",
															onClick: () => G(e.orderSeq, t),
															className: `btn-action py-2.5 border text-sm shadow-sm transition-all ${t.deliveryStatus === "DELIVERED" ? "border-amber-200 text-amber-600 bg-amber-50 hover:bg-amber-100" : "border-slate-200 text-slate-500 bg-white hover:bg-slate-50"}`,
															children: "반품신청"
														}) : /* @__PURE__ */ (0, o.jsx)("button", {
															type: "button",
															disabled: !0,
															class: "btn-action py-2.5 border border-slate-200 text-slate-400 bg-slate-50 text-sm shadow-sm cursor-not-allowed",
															children: "반품 불가"
														}),
														i ? /* @__PURE__ */ (0, o.jsx)("button", {
															type: "button",
															onClick: () => W(e.orderSeq, t),
															className: `btn-action py-2.5 border text-sm shadow-sm transition-all ${t.deliveryStatus === "READY" ? "border-red-200 text-red-600 bg-red-50 hover:bg-red-100" : t.deliveryStatus === "DELIVERED" ? "border-slate-200 text-slate-400 bg-slate-50 cursor-not-allowed" : "border-slate-200 text-slate-500 bg-white hover:bg-slate-50"}`,
															children: "주문취소"
														}) : /* @__PURE__ */ (0, o.jsx)("button", {
															type: "button",
															disabled: !0,
															class: "btn-action py-2.5 border border-slate-200 text-slate-400 bg-slate-50 text-sm shadow-sm cursor-not-allowed",
															children: "취소 완료"
														})
													]
												})
											})]
										})]
									}, n);
								})
							})]
						}, e.orderSeq)),
						u && j && n && /* @__PURE__ */ (0, o.jsx)("div", {
							className: "text-center pt-4 text-slate-500 font-bold text-sm",
							children: "로딩 중..."
						}),
						u && !j && /* @__PURE__ */ (0, o.jsx)("div", {
							className: "text-center pt-4",
							children: /* @__PURE__ */ (0, o.jsxs)("button", {
								type: "button",
								disabled: n,
								onClick: R,
								className: "px-8 py-3 bg-white border border-slate-200 text-slate-700 rounded-xl font-bold text-sm hover:bg-slate-50 transition-colors shadow-sm inline-flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed",
								children: [
									n ? "로딩 중..." : "더보기",
									" ",
									/* @__PURE__ */ (0, o.jsx)("svg", {
										className: "w-4 h-4",
										fill: "none",
										stroke: "currentColor",
										viewBox: "0 0 24 24",
										children: /* @__PURE__ */ (0, o.jsx)("path", {
											strokeLinecap: "round",
											strokeLinejoin: "round",
											strokeWidth: "2",
											d: "M19 9l-7 7-7-7"
										})
									})
								]
							})
						})
					]
				}) : n ? /* @__PURE__ */ (0, o.jsx)("div", {
					class: "bg-white p-12 text-center border border-slate-200 rounded-2xl shadow-sm",
					children: /* @__PURE__ */ (0, o.jsx)("p", {
						class: "text-slate-500 text-sm",
						children: "주문 정보를 불러오는 중입니다..."
					})
				}) : /* @__PURE__ */ (0, o.jsxs)("div", {
					class: "premium-card p-12 text-center border border-slate-200 rounded-2xl shadow-sm bg-white",
					children: [
						/* @__PURE__ */ (0, o.jsx)("div", {
							class: "w-20 h-20 bg-slate-100 rounded-full flex items-center justify-center mx-auto mb-4",
							children: /* @__PURE__ */ (0, o.jsx)("svg", {
								class: "w-10 h-10 text-slate-400",
								fill: "none",
								stroke: "currentColor",
								viewBox: "0 0 24 24",
								children: /* @__PURE__ */ (0, o.jsx)("path", {
									"stroke-linecap": "round",
									"stroke-linejoin": "round",
									"stroke-width": "1.5",
									d: "M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"
								})
							})
						}),
						/* @__PURE__ */ (0, o.jsx)("h3", {
							class: "text-lg font-bold text-slate-700",
							children: "주문 내역이 없습니다."
						}),
						/* @__PURE__ */ (0, o.jsx)("p", {
							class: "text-slate-500 mt-2 text-sm",
							children: "새로운 상품을 둘러보고 마음에 드는 상품을 주문해보세요."
						}),
						/* @__PURE__ */ (0, o.jsx)("a", {
							href: "/",
							class: "inline-block mt-6 px-6 py-2.5 bg-slate-900 text-white rounded-xl font-bold text-sm hover:bg-slate-800 transition-colors",
							children: "쇼핑하러 가기"
						})
					]
				})
			}),
			/* @__PURE__ */ (0, o.jsx)("div", {
				id: "trackModal",
				className: `modal-overlay ${f ? "active" : ""}`,
				onClick: (e) => e.target.id === "trackModal" && B(),
				children: /* @__PURE__ */ (0, o.jsxs)("div", {
					class: "modal-content",
					children: [
						/* @__PURE__ */ (0, o.jsx)("h3", {
							class: "text-xl font-bold text-slate-900 mb-6",
							children: "배송 상태 조회"
						}),
						/* @__PURE__ */ (0, o.jsx)("div", {
							class: "mb-8 px-2",
							children: /* @__PURE__ */ (0, o.jsxs)("div", {
								class: "flex items-center justify-between relative",
								children: [
									/* @__PURE__ */ (0, o.jsx)("div", { class: "absolute left-0 right-0 top-5 -translate-y-1/2 h-1 bg-slate-100 -z-10" }),
									/* @__PURE__ */ (0, o.jsx)("div", {
										id: "stepProgress",
										class: "absolute left-0 top-5 -translate-y-1/2 h-1 bg-blue-500 transition-all duration-500 -z-10",
										style: { width: g.stepProgressWidth }
									}),
									/* @__PURE__ */ (0, o.jsxs)("div", {
										id: "step1",
										class: "flex flex-col items-center flex-1 relative z-10",
										children: [/* @__PURE__ */ (0, o.jsx)("div", {
											className: `step-circle w-10 h-10 rounded-full flex items-center justify-center border-2 font-bold transition-all duration-300 ${g.step1Active ? "bg-blue-500 border-blue-500 text-white shadow-md shadow-blue-100" : "bg-slate-100 border-slate-200 text-slate-400"}`,
											children: /* @__PURE__ */ (0, o.jsx)("svg", {
												class: "w-5 h-5",
												fill: "none",
												stroke: "currentColor",
												viewBox: "0 0 24 24",
												children: /* @__PURE__ */ (0, o.jsx)("path", {
													"stroke-linecap": "round",
													"stroke-linejoin": "round",
													"stroke-width": "2",
													d: "M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"
												})
											})
										}), /* @__PURE__ */ (0, o.jsx)("span", {
											className: `step-label text-xs font-bold mt-2 transition-all ${g.step1Active ? "text-blue-600" : "text-slate-400"}`,
											children: "배송대기"
										})]
									}),
									/* @__PURE__ */ (0, o.jsxs)("div", {
										id: "step2",
										class: "flex flex-col items-center flex-1 relative z-10",
										children: [/* @__PURE__ */ (0, o.jsx)("div", {
											className: `step-circle w-10 h-10 rounded-full flex items-center justify-center border-2 font-bold transition-all duration-300 ${g.step2Active ? "bg-blue-500 border-blue-500 text-white shadow-md shadow-blue-100" : "bg-slate-100 border-slate-200 text-slate-400"}`,
											children: /* @__PURE__ */ (0, o.jsx)("svg", {
												class: "w-5 h-5",
												fill: "none",
												stroke: "currentColor",
												viewBox: "0 0 24 24",
												children: /* @__PURE__ */ (0, o.jsx)("path", {
													"stroke-linecap": "round",
													"stroke-linejoin": "round",
													"stroke-width": "2",
													d: "M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"
												})
											})
										}), /* @__PURE__ */ (0, o.jsx)("span", {
											className: `step-label text-xs font-bold mt-2 transition-all ${g.step2Active ? "text-blue-600" : "text-slate-400"}`,
											children: "중간허브도착"
										})]
									}),
									/* @__PURE__ */ (0, o.jsxs)("div", {
										id: "step3",
										class: "flex flex-col items-center flex-1 relative z-10",
										children: [/* @__PURE__ */ (0, o.jsx)("div", {
											className: `step-circle w-10 h-10 rounded-full flex items-center justify-center border-2 font-bold transition-all duration-300 ${g.step3Active ? "bg-blue-500 border-blue-500 text-white shadow-md shadow-blue-100" : "bg-slate-100 border-slate-200 text-slate-400"}`,
											children: /* @__PURE__ */ (0, o.jsx)("svg", {
												class: "w-5 h-5",
												fill: "none",
												stroke: "currentColor",
												viewBox: "0 0 24 24",
												children: /* @__PURE__ */ (0, o.jsx)("path", {
													"stroke-linecap": "round",
													"stroke-linejoin": "round",
													"stroke-width": "2",
													d: "M5 13l4 4L19 7"
												})
											})
										}), /* @__PURE__ */ (0, o.jsx)("span", {
											className: `step-label text-xs font-bold mt-2 transition-all ${g.step3Active ? "text-blue-600" : "text-slate-400"}`,
											children: "배송완료"
										})]
									})
								]
							})
						}),
						/* @__PURE__ */ (0, o.jsxs)("p", {
							class: "text-slate-500 text-sm mb-6",
							children: ["현재 배송 상태: ", /* @__PURE__ */ (0, o.jsxs)("span", {
								id: "modalStatusText",
								className: `font-bold ${g.status === "CANCELED" || g.status === "FAILED" ? "text-red-600" : g.status === "DELAYED" ? "text-orange-600" : "text-blue-600"}`,
								children: [
									g.status === "READY" && "배송대기",
									g.status === "SHIPPING" && "배송중",
									g.status === "DELAYED" && "배송지연",
									g.status === "DELIVERED" && "배송완료",
									g.status === "CANCELED" && "주문취소",
									g.status === "FAILED" && "배송실패"
								]
							})]
						}),
						/* @__PURE__ */ (0, o.jsxs)("div", {
							class: "bg-slate-50 p-4 rounded-xl border border-slate-200 mb-6",
							children: [/* @__PURE__ */ (0, o.jsxs)("p", {
								class: "text-xs font-bold text-slate-400 mb-1 whitespace-nowrap",
								children: ["운송장 번호 ", /* @__PURE__ */ (0, o.jsx)("span", {
									id: "modalCompanyName",
									class: "font-semibold text-slate-500 ml-1",
									children: g.companyName
								})]
							}), /* @__PURE__ */ (0, o.jsx)("p", {
								id: "modalTrackingNumber",
								class: "text-lg font-black tracking-widest text-slate-800 select-all",
								children: g.trackingNumber
							})]
						}),
						/* @__PURE__ */ (0, o.jsx)("button", {
							onClick: B,
							class: "w-full bg-slate-900 hover:bg-slate-800 text-white font-bold py-3.5 rounded-xl transition-colors text-sm shadow-md",
							children: "확인"
						})
					]
				})
			}),
			/* @__PURE__ */ (0, o.jsx)("div", {
				id: "cancelAlertModal",
				className: `modal-overlay ${y ? "active" : ""}`,
				onClick: (e) => e.target.id === "cancelAlertModal" && b(!1),
				children: /* @__PURE__ */ (0, o.jsxs)("div", {
					class: "modal-content",
					children: [
						/* @__PURE__ */ (0, o.jsx)("div", {
							class: "w-16 h-16 bg-red-50 text-red-500 rounded-full flex items-center justify-center mx-auto mb-4 border border-red-100",
							children: /* @__PURE__ */ (0, o.jsx)("svg", {
								class: "w-8 h-8",
								fill: "none",
								stroke: "currentColor",
								viewBox: "0 0 24 24",
								children: /* @__PURE__ */ (0, o.jsx)("path", {
									"stroke-linecap": "round",
									"stroke-linejoin": "round",
									"stroke-width": "2",
									d: "M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
								})
							})
						}),
						/* @__PURE__ */ (0, o.jsx)("h3", {
							class: "text-xl font-bold text-slate-900 mb-2",
							children: "취소 불가 안내"
						}),
						/* @__PURE__ */ (0, o.jsx)("p", {
							class: "text-slate-500 text-sm mb-6 leading-relaxed",
							children: x
						}),
						/* @__PURE__ */ (0, o.jsx)("button", {
							onClick: () => b(!1),
							class: "w-full bg-slate-900 hover:bg-slate-800 text-white font-bold py-3.5 rounded-xl transition-colors text-sm",
							children: "확인"
						})
					]
				})
			}),
			/* @__PURE__ */ (0, o.jsx)("div", {
				id: "returnAlertModal",
				className: `modal-overlay ${T ? "active" : ""}`,
				onClick: (e) => e.target.id === "returnAlertModal" && E(!1),
				children: /* @__PURE__ */ (0, o.jsxs)("div", {
					class: "modal-content",
					children: [
						/* @__PURE__ */ (0, o.jsx)("div", {
							class: "w-16 h-16 bg-amber-50 text-amber-500 rounded-full flex items-center justify-center mx-auto mb-4 border border-amber-100",
							children: /* @__PURE__ */ (0, o.jsx)("svg", {
								class: "w-8 h-8",
								fill: "none",
								stroke: "currentColor",
								viewBox: "0 0 24 24",
								children: /* @__PURE__ */ (0, o.jsx)("path", {
									"stroke-linecap": "round",
									"stroke-linejoin": "round",
									"stroke-width": "2",
									d: "M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
								})
							})
						}),
						/* @__PURE__ */ (0, o.jsx)("h3", {
							class: "text-xl font-bold text-slate-900 mb-2",
							children: "반품신청 불가"
						}),
						/* @__PURE__ */ (0, o.jsxs)("p", {
							class: "text-slate-500 text-sm mb-6 leading-relaxed",
							children: [
								"반품 신청은 배송이 완료된 이후에만",
								/* @__PURE__ */ (0, o.jsx)("br", {}),
								"가능합니다."
							]
						}),
						/* @__PURE__ */ (0, o.jsx)("button", {
							onClick: () => E(!1),
							class: "w-full bg-slate-900 hover:bg-slate-800 text-white font-bold py-3.5 rounded-xl transition-colors text-sm",
							children: "확인"
						})
					]
				})
			}),
			/* @__PURE__ */ (0, o.jsx)("div", {
				id: "returnNoSelectionModal",
				className: `modal-overlay ${C ? "active" : ""}`,
				onClick: (e) => e.target.id === "returnNoSelectionModal" && w(!1),
				children: /* @__PURE__ */ (0, o.jsxs)("div", {
					class: "modal-content",
					children: [
						/* @__PURE__ */ (0, o.jsx)("div", {
							class: "w-16 h-16 bg-amber-50 text-amber-500 rounded-full flex items-center justify-center mx-auto mb-4 border border-amber-100",
							children: /* @__PURE__ */ (0, o.jsx)("svg", {
								class: "w-8 h-8",
								fill: "none",
								stroke: "currentColor",
								viewBox: "0 0 24 24",
								children: /* @__PURE__ */ (0, o.jsx)("path", {
									"stroke-linecap": "round",
									"stroke-linejoin": "round",
									"stroke-width": "2",
									d: "M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
								})
							})
						}),
						/* @__PURE__ */ (0, o.jsx)("h3", {
							class: "text-xl font-bold text-slate-900 mb-2",
							children: "상품 미선택"
						}),
						/* @__PURE__ */ (0, o.jsxs)("p", {
							class: "text-slate-500 text-sm mb-6 leading-relaxed",
							children: [
								"반품 신청을 진행할 상품을",
								/* @__PURE__ */ (0, o.jsx)("br", {}),
								"최소 하나 이상 체크해 주세요."
							]
						}),
						/* @__PURE__ */ (0, o.jsx)("button", {
							onClick: () => w(!1),
							class: "w-full bg-slate-900 hover:bg-slate-800 text-white font-bold py-3.5 rounded-xl transition-colors text-sm",
							children: "확인"
						})
					]
				})
			}),
			/* @__PURE__ */ (0, o.jsx)("div", {
				id: "confirmModal",
				className: `modal-overlay ${D.isOpen ? "active" : ""}`,
				onClick: (e) => {
					e.target.id === "confirmModal" && D.step !== "PROCESSING" && O((e) => ({
						...e,
						isOpen: !1
					}));
				},
				children: /* @__PURE__ */ (0, o.jsxs)("div", {
					class: "modal-content",
					children: [
						D.step === "CONFIRM" && /* @__PURE__ */ (0, o.jsxs)("div", {
							id: "confirmStep",
							children: [
								/* @__PURE__ */ (0, o.jsx)("div", {
									className: `w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4 border ${D.type === "CANCEL" ? "bg-red-50 border-red-100 text-red-500" : "bg-amber-50 border-amber-100 text-amber-500"}`,
									children: /* @__PURE__ */ (0, o.jsx)("svg", {
										class: "w-8 h-8",
										fill: "none",
										stroke: "currentColor",
										viewBox: "0 0 24 24",
										children: /* @__PURE__ */ (0, o.jsx)("path", {
											"stroke-linecap": "round",
											"stroke-linejoin": "round",
											"stroke-width": "2",
											d: "M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
										})
									})
								}),
								/* @__PURE__ */ (0, o.jsx)("h3", {
									class: "text-xl font-bold text-slate-900 mb-2",
									children: D.title
								}),
								/* @__PURE__ */ (0, o.jsx)("p", {
									class: "text-slate-500 text-sm mb-6 leading-relaxed",
									dangerouslySetInnerHTML: { __html: D.text }
								}),
								/* @__PURE__ */ (0, o.jsxs)("div", {
									class: "mb-6 text-left",
									children: [/* @__PURE__ */ (0, o.jsx)("label", {
										class: "block text-xs font-bold text-slate-500 mb-2",
										children: D.type === "CANCEL" ? "취소 사유" : "반품 사유"
									}), /* @__PURE__ */ (0, o.jsx)("textarea", {
										value: N,
										onChange: (e) => P(e.target.value),
										placeholder: D.type === "CANCEL" ? "취소 사유를 적어주세요." : "반품 사유를 적어주세요.",
										class: "w-full p-3 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-amber-500 focus:bg-white transition-all resize-none h-20"
									})]
								}),
								/* @__PURE__ */ (0, o.jsxs)("div", {
									class: "flex gap-3",
									children: [/* @__PURE__ */ (0, o.jsx)("button", {
										onClick: () => O((e) => ({
											...e,
											isOpen: !1
										})),
										class: "flex-1 bg-slate-100 hover:bg-slate-200 text-slate-600 font-bold py-3.5 rounded-xl transition-colors text-sm",
										children: "아니오"
									}), /* @__PURE__ */ (0, o.jsx)("button", {
										onClick: D.type === "CANCEL" ? H : U,
										className: `flex-1 text-white font-bold py-3.5 rounded-xl transition-colors text-sm shadow-md ${D.type === "CANCEL" ? "bg-red-500 hover:bg-red-600 shadow-red-100" : "bg-amber-500 hover:bg-amber-600 shadow-amber-100"}`,
										children: "예, 진행합니다"
									})]
								})
							]
						}),
						D.step === "PROCESSING" && /* @__PURE__ */ (0, o.jsxs)("div", {
							id: "processingStep",
							class: "py-6",
							children: [
								/* @__PURE__ */ (0, o.jsx)("div", { class: "w-12 h-12 border-4 border-slate-200 border-t-amber-500 rounded-full animate-spin mx-auto mb-4" }),
								/* @__PURE__ */ (0, o.jsx)("h3", {
									class: "text-lg font-bold text-slate-900 mb-1",
									children: "요청을 처리하고 있습니다"
								}),
								/* @__PURE__ */ (0, o.jsx)("p", {
									class: "text-slate-400 text-xs",
									children: "잠시만 기다려 주세요..."
								})
							]
						}),
						D.step === "RESULT" && /* @__PURE__ */ (0, o.jsxs)("div", {
							id: "resultStep",
							children: [
								/* @__PURE__ */ (0, o.jsx)("div", {
									className: `w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4 border ${D.isSuccess ? "bg-emerald-50 border-emerald-100 text-emerald-500" : "bg-red-50 border-red-100 text-red-500"}`,
									children: D.isSuccess ? /* @__PURE__ */ (0, o.jsx)("svg", {
										class: "w-8 h-8",
										fill: "none",
										stroke: "currentColor",
										viewBox: "0 0 24 24",
										children: /* @__PURE__ */ (0, o.jsx)("path", {
											"stroke-linecap": "round",
											"stroke-linejoin": "round",
											"stroke-width": "2",
											d: "M5 13l4 4L19 7"
										})
									}) : /* @__PURE__ */ (0, o.jsx)("svg", {
										class: "w-8 h-8",
										fill: "none",
										stroke: "currentColor",
										viewBox: "0 0 24 24",
										children: /* @__PURE__ */ (0, o.jsx)("path", {
											"stroke-linecap": "round",
											"stroke-linejoin": "round",
											"stroke-width": "2",
											d: "M6 18L18 6M6 6l12 12"
										})
									})
								}),
								/* @__PURE__ */ (0, o.jsx)("h3", {
									class: "text-xl font-bold text-slate-900 mb-2",
									children: D.resultTitle
								}),
								/* @__PURE__ */ (0, o.jsx)("p", {
									class: "text-slate-500 text-sm mb-6 leading-relaxed",
									children: D.resultMessage
								}),
								/* @__PURE__ */ (0, o.jsx)("button", {
									onClick: () => {
										O((e) => ({
											...e,
											isOpen: !1
										})), D.isSuccess && F(a, c);
									},
									class: "w-full bg-slate-900 hover:bg-slate-800 text-white font-bold py-3.5 rounded-xl transition-colors text-sm",
									children: "확인"
								})
							]
						})
					]
				})
			})
		]
	});
}
var c = document.getElementById("orders-root");
c && (0, a.createRoot)(c).render(/* @__PURE__ */ (0, o.jsx)(s, {}));
//#endregion
