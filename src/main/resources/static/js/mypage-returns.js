import { i as e, n as t, r as n, t as r } from "./jsx-runtime-DGhihkRN.js";
//#region src/mypage-returns.jsx
var i = /* @__PURE__ */ e(n(), 1), a = t(), o = r();
function s() {
	let [e, t] = (0, i.useState)([]), [n, r] = (0, i.useState)(!0), [a, s] = (0, i.useState)(!1), [c, l] = (0, i.useState)({
		type: "CANCEL",
		title: "반품 상세 정보",
		labelRequestDate: "반품접수일자",
		labelUid: "반품접수번호",
		labelReasonHeader: "반품 사유",
		showDeliveryFee: !0,
		thumbnailUrl: "",
		productName: "",
		productPrice: 0,
		quantity: 0,
		statusText: "",
		requestDate: "",
		uid: "",
		completedDate: "",
		reason: "",
		originalPrice: 0,
		discountPrice: 0,
		deliveryFee: 0,
		refundPrice: 0,
		paymentMethod: ""
	}), u = (0, i.useRef)(null), d = async () => {
		r(!0);
		try {
			let e = await fetch("/api/mypage/returns");
			if (!e.ok) throw Error("데이터 수집 실패");
			t(await e.json());
		} catch (e) {
			console.error("취소/반품 내역 로드 중 에러: ", e);
		} finally {
			r(!1);
		}
	};
	(0, i.useEffect)(() => {
		d();
	}, []);
	let f = (e) => {
		u.current &&= (clearTimeout(u.current), null);
		let t = e.type === "CANCEL";
		l({
			type: e.type,
			title: t ? "취소 상세 정보" : "반품 상세 정보",
			labelRequestDate: t ? "취소접수일자" : "반품접수일자",
			labelUid: t ? "취소접수번호" : "반품접수번호",
			labelReasonHeader: t ? "취소 사유" : "반품 사유",
			showDeliveryFee: !t,
			thumbnailUrl: e.thumbnailUrl || "/images/default-product.png",
			productName: e.productName,
			productPrice: e.productPrice || 0,
			quantity: e.quantity || 0,
			statusText: e.statusText,
			requestDate: e.requestDate || "-",
			uid: e.uid || "-",
			completedDate: e.completedDate || "진행 중",
			reason: e.reason || "-",
			originalPrice: e.originalPrice || (e.productPrice || 0) * (e.quantity || 0),
			discountPrice: e.discountPrice || 0,
			deliveryFee: e.deliveryFee || 0,
			refundPrice: e.refundPrice || 0,
			paymentMethod: e.paymentMethod || "토스 페이먼츠"
		}), s(!0);
	}, p = () => {
		s(!1), u.current && clearTimeout(u.current), u.current = setTimeout(() => {
			l({
				type: "CANCEL",
				title: "반품 상세 정보",
				labelRequestDate: "반품접수일자",
				labelUid: "반품접수번호",
				labelReasonHeader: "반품 사유",
				showDeliveryFee: !0,
				thumbnailUrl: "",
				productName: "",
				productPrice: 0,
				quantity: 0,
				statusText: "",
				requestDate: "",
				uid: "",
				completedDate: "",
				reason: "",
				originalPrice: 0,
				discountPrice: 0,
				deliveryFee: 0,
				refundPrice: 0,
				paymentMethod: ""
			}), u.current = null;
		}, 150);
	}, m = (e) => (e || 0).toLocaleString("ko-KR") + "원";
	return /* @__PURE__ */ (0, o.jsxs)("section", {
		className: "space-y-6",
		children: [
			/* @__PURE__ */ (0, o.jsx)("div", {
				className: "flex justify-between items-end mb-8",
				children: /* @__PURE__ */ (0, o.jsxs)("div", { children: [/* @__PURE__ */ (0, o.jsx)("h1", {
					className: "text-2xl font-extrabold text-slate-900",
					children: "취소/반품/교환/환불내역"
				}), /* @__PURE__ */ (0, o.jsx)("p", {
					className: "text-slate-500 mt-2 text-sm",
					children: "신청하신 주문 취소 및 반품 진행 내역을 확인할 수 있습니다."
				})] })
			}),
			/* @__PURE__ */ (0, o.jsx)("div", {
				className: "space-y-5",
				children: n ? /* @__PURE__ */ (0, o.jsx)("div", {
					className: "bg-white p-12 text-center border border-slate-200 rounded-2xl shadow-sm",
					children: /* @__PURE__ */ (0, o.jsx)("p", {
						className: "text-slate-500 text-sm",
						children: "정보를 불러오는 중입니다..."
					})
				}) : e && e.length > 0 ? e.map((e, t) => {
					let n = e.type === "CANCEL", r = "status-badge font-extrabold ";
					return n ? r += "status-CANCEL" : e.itemStatus === 9 ? r += "status-RETURNED" : r += "status-RETURN", /* @__PURE__ */ (0, o.jsxs)("article", {
						className: "premium-card overflow-hidden",
						children: [/* @__PURE__ */ (0, o.jsxs)("div", {
							className: "px-6 py-4 bg-slate-50/50 border-b border-slate-100 flex justify-between items-center flex-wrap gap-2 text-sm",
							children: [/* @__PURE__ */ (0, o.jsxs)("div", {
								className: "flex items-center gap-3",
								children: [/* @__PURE__ */ (0, o.jsx)("span", {
									className: "text-slate-400 font-bold",
									children: n ? "주문취소 접수일" : "반품 접수일"
								}), /* @__PURE__ */ (0, o.jsx)("span", {
									className: "font-bold text-slate-700",
									children: e.requestDate
								})]
							}), /* @__PURE__ */ (0, o.jsxs)("div", {
								className: "flex items-center gap-3 text-slate-400",
								children: [
									/* @__PURE__ */ (0, o.jsx)("span", { children: "주문번호:" }),
									/* @__PURE__ */ (0, o.jsx)("span", {
										className: "font-semibold text-slate-600",
										children: e.orderUid
									}),
									e.deliveryCompany && /* @__PURE__ */ (0, o.jsx)("span", {
										className: "text-xs font-bold text-amber-600 bg-amber-50 border border-amber-100 px-2 py-0.5 rounded-full",
										children: e.deliveryCompany
									})
								]
							})]
						}), /* @__PURE__ */ (0, o.jsxs)("div", {
							className: "p-6 flex flex-col lg:flex-row justify-between items-start lg:items-center gap-6",
							children: [/* @__PURE__ */ (0, o.jsx)("div", {
								className: "flex-1 space-y-4 w-full",
								children: e.items && e.items.length > 0 ? e.items.map((e, t) => /* @__PURE__ */ (0, o.jsxs)("div", {
									className: "flex gap-5 items-center pb-3 last:pb-0 border-b border-slate-100/50 last:border-b-0",
									children: [/* @__PURE__ */ (0, o.jsx)("div", {
										className: "relative w-16 h-16 rounded-xl overflow-hidden shadow-sm border border-slate-100 shrink-0",
										children: /* @__PURE__ */ (0, o.jsx)("img", {
											src: e.thumbnailUrl || "/images/default-product.png",
											alt: "상품 이미지",
											className: "w-full h-full object-cover"
										})
									}), /* @__PURE__ */ (0, o.jsxs)("div", {
										className: "space-y-0.5",
										children: [/* @__PURE__ */ (0, o.jsx)("h4", {
											className: "font-bold text-sm text-slate-950 leading-tight",
											children: e.productName
										}), /* @__PURE__ */ (0, o.jsxs)("p", {
											className: "text-xs text-slate-500 font-medium",
											children: [
												/* @__PURE__ */ (0, o.jsx)("span", { children: m(e.productPrice) }),
												/* @__PURE__ */ (0, o.jsx)("span", {
													className: "mx-1",
													children: "/"
												}),
												/* @__PURE__ */ (0, o.jsxs)("span", { children: [e.quantity, "개"] })
											]
										})]
									})]
								}, t)) : /* @__PURE__ */ (0, o.jsxs)("div", {
									className: "flex gap-5 items-center",
									children: [/* @__PURE__ */ (0, o.jsx)("div", {
										className: "relative w-16 h-16 rounded-xl overflow-hidden shadow-sm border border-slate-100 shrink-0",
										children: /* @__PURE__ */ (0, o.jsx)("img", {
											src: e.thumbnailUrl || "/images/default-product.png",
											alt: "상품 이미지",
											className: "w-full h-full object-cover"
										})
									}), /* @__PURE__ */ (0, o.jsxs)("div", {
										className: "space-y-0.5",
										children: [/* @__PURE__ */ (0, o.jsx)("h4", {
											className: "font-bold text-sm text-slate-950 leading-tight",
											children: e.productName
										}), /* @__PURE__ */ (0, o.jsxs)("p", {
											className: "text-xs text-slate-500 font-medium",
											children: [
												/* @__PURE__ */ (0, o.jsx)("span", { children: m(e.productPrice) }),
												/* @__PURE__ */ (0, o.jsx)("span", {
													className: "mx-1",
													children: "/"
												}),
												/* @__PURE__ */ (0, o.jsxs)("span", { children: [e.quantity, "개"] })
											]
										})]
									})]
								})
							}), /* @__PURE__ */ (0, o.jsxs)("div", {
								className: "w-full lg:w-auto flex justify-between lg:justify-end items-center gap-6 self-stretch lg:self-auto border-t lg:border-t-0 pt-4 lg:pt-0 border-slate-100",
								children: [/* @__PURE__ */ (0, o.jsxs)("div", {
									className: "flex flex-col items-start lg:items-center gap-1",
									children: [/* @__PURE__ */ (0, o.jsx)("span", {
										className: "text-xs text-slate-400 font-bold",
										children: "진행 상태"
									}), /* @__PURE__ */ (0, o.jsx)("span", {
										className: r,
										children: e.statusText
									})]
								}), /* @__PURE__ */ (0, o.jsx)("button", {
									type: "button",
									onClick: () => f(e),
									className: "btn-detail py-2.5 px-6 border text-slate-700 bg-white border-slate-200 hover:border-amber-500 hover:text-amber-600 text-sm shadow-sm shrink-0",
									children: /* @__PURE__ */ (0, o.jsx)("span", { children: n ? "취소 상세" : "반품 상세" })
								})]
							})]
						})]
					}, e.orderItemSeq || t);
				}) : /* @__PURE__ */ (0, o.jsxs)("div", {
					className: "premium-card p-12 text-center bg-white",
					children: [
						/* @__PURE__ */ (0, o.jsx)("div", {
							className: "w-20 h-20 bg-slate-100 rounded-full flex items-center justify-center mx-auto mb-4",
							children: /* @__PURE__ */ (0, o.jsx)("svg", {
								className: "w-10 h-10 text-slate-400",
								fill: "none",
								stroke: "currentColor",
								viewBox: "0 0 24 24",
								children: /* @__PURE__ */ (0, o.jsx)("path", {
									"stroke-linecap": "round",
									"stroke-linejoin": "round",
									"stroke-width": "1.5",
									d: "M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01"
								})
							})
						}),
						/* @__PURE__ */ (0, o.jsx)("h3", {
							className: "text-lg font-bold text-slate-700",
							children: "취소/반품 내역이 없습니다."
						}),
						/* @__PURE__ */ (0, o.jsx)("p", {
							className: "text-slate-500 mt-2 text-sm",
							children: "신청 완료된 취소 및 반품 내역이 이곳에 표시됩니다."
						})
					]
				})
			}),
			/* @__PURE__ */ (0, o.jsx)("div", {
				id: "detailModal",
				className: `modal-overlay ${a ? "active" : ""}`,
				onClick: (e) => e.target.id === "detailModal" && p(),
				children: /* @__PURE__ */ (0, o.jsxs)("div", {
					className: "modal-content text-left space-y-6 relative max-h-[90vh] overflow-y-auto",
					children: [
						/* @__PURE__ */ (0, o.jsx)("button", {
							onClick: p,
							className: "absolute top-4 right-4 text-slate-400 hover:text-slate-600",
							children: /* @__PURE__ */ (0, o.jsx)("svg", {
								className: "w-6 h-6",
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
							className: "text-xl font-extrabold text-slate-900 border-b border-slate-100 pb-3",
							children: c.title
						}),
						/* @__PURE__ */ (0, o.jsxs)("div", {
							className: "flex gap-4 items-center bg-slate-50 p-4 rounded-2xl border border-slate-100",
							children: [/* @__PURE__ */ (0, o.jsx)("div", {
								className: "w-16 h-16 rounded-xl overflow-hidden shadow-sm shrink-0 border border-slate-200",
								children: /* @__PURE__ */ (0, o.jsx)("img", {
									src: c.thumbnailUrl,
									alt: "상품 이미지",
									className: "w-full h-full object-cover"
								})
							}), /* @__PURE__ */ (0, o.jsxs)("div", {
								className: "space-y-0.5",
								children: [
									/* @__PURE__ */ (0, o.jsx)("h4", {
										className: "font-bold text-sm text-slate-900 leading-tight",
										children: c.productName
									}),
									/* @__PURE__ */ (0, o.jsxs)("p", {
										className: "text-xs text-slate-500 font-medium",
										children: [
											/* @__PURE__ */ (0, o.jsx)("span", { children: m(c.productPrice) }),
											" / ",
											/* @__PURE__ */ (0, o.jsxs)("span", { children: [c.quantity, "개"] })
										]
									}),
									/* @__PURE__ */ (0, o.jsx)("div", {
										className: "pt-1",
										children: /* @__PURE__ */ (0, o.jsx)("span", {
											className: `status-badge text-[10px] font-black ${c.type === "CANCEL" ? "status-CANCEL" : c.statusText === "반품완료" ? "status-RETURNED" : "status-RETURN"}`,
											children: c.statusText
										})
									})
								]
							})]
						}),
						/* @__PURE__ */ (0, o.jsxs)("div", {
							className: "space-y-3",
							children: [/* @__PURE__ */ (0, o.jsxs)("h4", {
								className: "font-bold text-sm text-slate-900 flex items-center gap-1.5",
								children: [/* @__PURE__ */ (0, o.jsx)("span", { className: "w-1 h-3 bg-amber-500 rounded-full" }), "접수 정보"]
							}), /* @__PURE__ */ (0, o.jsxs)("div", {
								className: "bg-slate-50 p-4 rounded-2xl border border-slate-100 text-xs space-y-2.5 text-slate-600 font-medium",
								children: [
									/* @__PURE__ */ (0, o.jsxs)("div", {
										className: "flex justify-between",
										children: [/* @__PURE__ */ (0, o.jsx)("span", { children: c.labelRequestDate }), /* @__PURE__ */ (0, o.jsx)("span", {
											className: "text-slate-900 font-bold",
											children: c.requestDate
										})]
									}),
									/* @__PURE__ */ (0, o.jsxs)("div", {
										className: "flex justify-between",
										children: [/* @__PURE__ */ (0, o.jsx)("span", { children: c.labelUid }), /* @__PURE__ */ (0, o.jsx)("span", {
											className: "text-slate-900 font-bold tracking-wider",
											children: c.uid
										})]
									}),
									/* @__PURE__ */ (0, o.jsxs)("div", {
										className: "flex justify-between",
										children: [/* @__PURE__ */ (0, o.jsx)("span", { children: "완료일자" }), /* @__PURE__ */ (0, o.jsx)("span", {
											className: "text-slate-900 font-bold",
											children: c.completedDate
										})]
									})
								]
							})]
						}),
						/* @__PURE__ */ (0, o.jsxs)("div", {
							className: "space-y-3",
							children: [/* @__PURE__ */ (0, o.jsxs)("h4", {
								className: "font-bold text-sm text-slate-900 flex items-center gap-1.5",
								children: [/* @__PURE__ */ (0, o.jsx)("span", { className: "w-1 h-3 bg-amber-500 rounded-full" }), c.labelReasonHeader]
							}), /* @__PURE__ */ (0, o.jsx)("div", {
								className: "bg-slate-50 p-4 rounded-2xl border border-slate-100 text-xs text-slate-800 font-bold",
								children: c.reason
							})]
						}),
						/* @__PURE__ */ (0, o.jsxs)("div", {
							className: "space-y-3",
							children: [/* @__PURE__ */ (0, o.jsxs)("h4", {
								className: "font-bold text-sm text-slate-900 flex items-center gap-1.5",
								children: [/* @__PURE__ */ (0, o.jsx)("span", { className: "w-1 h-3 bg-amber-500 rounded-full" }), "환불 안내"]
							}), /* @__PURE__ */ (0, o.jsxs)("div", {
								className: "bg-slate-50 p-4 rounded-2xl border border-slate-100 text-xs space-y-3 text-slate-600 font-medium",
								children: [
									/* @__PURE__ */ (0, o.jsxs)("div", {
										className: "flex justify-between",
										children: [/* @__PURE__ */ (0, o.jsx)("span", { children: "상품 금액" }), /* @__PURE__ */ (0, o.jsx)("span", {
											className: "text-slate-900 font-bold",
											children: m(c.originalPrice)
										})]
									}),
									/* @__PURE__ */ (0, o.jsxs)("div", {
										className: "flex justify-between",
										children: [/* @__PURE__ */ (0, o.jsx)("span", { children: "할인 금액" }), /* @__PURE__ */ (0, o.jsxs)("span", {
											className: "text-red-500 font-bold",
											children: ["-", m(c.discountPrice)]
										})]
									}),
									c.showDeliveryFee && /* @__PURE__ */ (0, o.jsxs)("div", {
										className: "flex justify-between",
										children: [/* @__PURE__ */ (0, o.jsx)("span", { children: "반품 배송비" }), /* @__PURE__ */ (0, o.jsxs)("span", {
											className: "text-slate-900 font-bold",
											children: ["-", m(c.deliveryFee)]
										})]
									}),
									/* @__PURE__ */ (0, o.jsxs)("div", {
										className: "border-t border-slate-200/60 pt-3 flex justify-between items-center text-sm font-extrabold",
										children: [/* @__PURE__ */ (0, o.jsx)("span", {
											className: "text-slate-900",
											children: "최종 환불 금액"
										}), /* @__PURE__ */ (0, o.jsx)("span", {
											className: "text-amber-600 font-black text-base",
											children: m(c.refundPrice)
										})]
									}),
									/* @__PURE__ */ (0, o.jsxs)("div", {
										className: "border-t border-slate-200/60 pt-3 flex justify-between",
										children: [/* @__PURE__ */ (0, o.jsx)("span", { children: "환불 수단" }), /* @__PURE__ */ (0, o.jsx)("span", {
											className: "text-slate-900 font-bold",
											children: c.paymentMethod
										})]
									}),
									/* @__PURE__ */ (0, o.jsxs)("div", {
										className: "flex justify-between",
										children: [/* @__PURE__ */ (0, o.jsx)("span", { children: "환불 상태" }), /* @__PURE__ */ (0, o.jsx)("span", {
											className: "text-emerald-600 font-bold",
											children: "환불 완료"
										})]
									})
								]
							})]
						}),
						/* @__PURE__ */ (0, o.jsx)("button", {
							onClick: p,
							className: "w-full bg-slate-900 hover:bg-slate-800 text-white font-bold py-3 rounded-xl transition-colors text-sm",
							children: "확인"
						})
					]
				})
			})
		]
	});
}
var c = document.getElementById("returns-root");
c && (0, a.createRoot)(c).render(/* @__PURE__ */ (0, o.jsx)(s, {}));
//#endregion
