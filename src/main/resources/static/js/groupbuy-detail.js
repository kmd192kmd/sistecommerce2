import { i as e, n as t, r as n, t as r } from "./jsx-runtime-DGhihkRN.js";
//#region src/groupbuy-detail.jsx
var i = /* @__PURE__ */ e(n(), 1), a = t(), o = r(), s = (e) => (e ?? 0).toLocaleString("ko-KR");
function c(e) {
	if (e <= 0) return "마감";
	let t = Math.floor(e / 86400), n = Math.floor(e % 86400 / 3600), r = Math.floor(e % 3600 / 60), i = e % 60;
	return t > 0 ? `${t}일 ${n}시간 ${r}분 ${i}초` : `${n}시간 ${r}분 ${i}초`;
}
function l({ id: e, data: t, toss: n }) {
	let [r, a] = (0, i.useState)(""), [l, u] = (0, i.useState)(t.remainSeconds), [d, f] = (0, i.useState)(!1), [p, m] = (0, i.useState)("");
	(0, i.useEffect)(() => {
		let e = setInterval(() => {
			u((e) => e > 0 ? e - 1 : 0);
		}, 1e3);
		return () => clearInterval(e);
	}, []);
	let h = l <= 0, g = t.options.find((e) => String(e.optionsSeq) === String(r)) || null, _ = g ? g.finalPrice : t.finalPrice;
	async function v() {
		if (!r) {
			m("옵션을 선택해주세요.");
			return;
		}
		f(!0), m("");
		try {
			let t = await fetch(`/api/group-buys/${e}/participate?optionSeq=${r}`, { method: "POST" });
			if (t.status === 401) {
				m("로그인이 필요합니다.");
				return;
			}
			if (!t.ok) {
				m((await t.json().catch(() => null))?.message || "참여 처리에 실패했습니다.");
				return;
			}
			let n = await t.json();
			if (n.result === "QUEUED") {
				m("매진되어 대기열에 등록되었습니다.");
				return;
			}
			await y(n);
		} catch (t) {
			if (t && (t.code === "USER_CANCEL" || t.code === "PAY_PROCESS_CANCELED")) {
				try {
					await fetch(`/api/group-buys/${e}/cancel-pending`, { method: "POST" });
				} catch {}
				m("결제를 취소했어요. 예약했던 자리를 반납했습니다.");
			} else m("결제 요청 중 오류가 발생했습니다.");
		} finally {
			f(!1);
		}
	}
	async function y(e) {
		if (!n || !n.clientKey) {
			m("결제 설정을 불러오지 못했습니다.");
			return;
		}
		await window.TossPayments(n.clientKey).payment({ customerKey: `member-${n.memberSeq}` }).requestPayment({
			method: "CARD",
			amount: {
				currency: "KRW",
				value: Number(e.amount)
			},
			orderId: e.orderUid,
			orderName: e.orderName,
			successUrl: n.successUrl,
			failUrl: n.failUrl
		});
	}
	return /* @__PURE__ */ (0, o.jsxs)("div", { children: [
		/* @__PURE__ */ (0, o.jsxs)("div", {
			className: "flex items-start justify-between",
			children: [/* @__PURE__ */ (0, o.jsx)("h1", {
				className: "text-2xl font-bold leading-snug",
				children: t.productName
			}), /* @__PURE__ */ (0, o.jsx)("span", {
				className: "shrink-0 bg-slate-900 text-amber-400 text-xs font-bold px-2 py-1",
				children: h ? "마감" : `마감까지 ${c(l)}`
			})]
		}),
		/* @__PURE__ */ (0, o.jsxs)("div", {
			className: "mt-6 space-y-1 border-y border-gray-100 py-5",
			children: [/* @__PURE__ */ (0, o.jsxs)("p", {
				className: "text-sm text-gray-400",
				children: ["정가 ", /* @__PURE__ */ (0, o.jsxs)("span", {
					className: "line-through",
					children: [s(t.originalPrice), "원"]
				})]
			}), /* @__PURE__ */ (0, o.jsxs)("p", {
				className: "flex items-baseline gap-2",
				children: [
					/* @__PURE__ */ (0, o.jsxs)("span", {
						className: "text-red-500 font-black text-2xl",
						children: [t.discountRate, "%"]
					}),
					/* @__PURE__ */ (0, o.jsx)("span", {
						className: "text-3xl font-black",
						children: s(_)
					}),
					/* @__PURE__ */ (0, o.jsx)("span", {
						className: "text-lg font-bold",
						children: "원"
					})
				]
			})]
		}),
		/* @__PURE__ */ (0, o.jsxs)("div", {
			className: "mt-5",
			children: [/* @__PURE__ */ (0, o.jsxs)("div", {
				className: "flex justify-between text-xs font-bold mb-1",
				children: [/* @__PURE__ */ (0, o.jsxs)("span", {
					className: "text-amber-600",
					children: [t.currentCount, "명 참여"]
				}), /* @__PURE__ */ (0, o.jsxs)("span", {
					className: "text-gray-400",
					children: [
						"목표 ",
						t.minCount,
						"명"
					]
				})]
			}), /* @__PURE__ */ (0, o.jsx)("div", {
				className: "w-full h-3 bg-gray-100 overflow-hidden",
				children: /* @__PURE__ */ (0, o.jsx)("div", {
					className: "h-full bg-amber-500",
					style: { width: `${t.progress}%` }
				})
			})]
		}),
		/* @__PURE__ */ (0, o.jsxs)("div", {
			className: "mt-6",
			children: [/* @__PURE__ */ (0, o.jsx)("label", {
				className: "block text-xs font-bold text-gray-500 mb-1",
				children: "옵션 선택"
			}), /* @__PURE__ */ (0, o.jsxs)("select", {
				className: "w-full border border-gray-300 px-3 py-2.5 text-sm focus:outline-none focus:border-amber-500",
				value: r,
				onChange: (e) => a(e.target.value),
				disabled: h,
				children: [/* @__PURE__ */ (0, o.jsx)("option", {
					value: "",
					children: "옵션을 선택하세요"
				}), t.options.map((e) => /* @__PURE__ */ (0, o.jsxs)("option", {
					value: e.optionsSeq,
					disabled: e.soldOut,
					children: [e.label, e.soldOut ? " (매진)" : ""]
				}, e.optionsSeq))]
			})]
		}),
		/* @__PURE__ */ (0, o.jsx)("button", {
			type: "button",
			onClick: v,
			disabled: h || d,
			className: "mt-4 w-full bg-amber-500 hover:bg-amber-600 text-white font-bold py-3 text-sm disabled:bg-gray-300 disabled:cursor-not-allowed",
			children: h ? "마감되었습니다" : d ? "처리 중…" : "바로구매 (공구 참여)"
		}),
		p && /* @__PURE__ */ (0, o.jsx)("p", {
			className: "mt-2 text-sm text-center text-amber-700 font-bold",
			children: p
		}),
		/* @__PURE__ */ (0, o.jsx)("p", {
			className: "mt-2 text-[11px] text-gray-400",
			children: "※ 공구 상품은 쿠폰·핫딜 적용 불가 / 1인 1상품, 수량 1개 고정"
		})
	] });
}
function u({ id: e, toss: t }) {
	let [n, r] = (0, i.useState)(null);
	return (0, i.useEffect)(() => {
		fetch(`/api/group-buys/${e}`).then((e) => e.json()).then(r);
	}, [e]), n ? /* @__PURE__ */ (0, o.jsx)(l, {
		id: e,
		data: n,
		toss: t
	}) : /* @__PURE__ */ (0, o.jsx)("p", {
		className: "text-gray-400",
		children: "불러오는 중…"
	});
}
var d = document.getElementById("gb-detail-root");
if (d) {
	let e = d.dataset.gbId, t = {
		clientKey: d.dataset.tossClientKey,
		successUrl: d.dataset.tossSuccessUrl,
		failUrl: d.dataset.tossFailUrl,
		memberSeq: d.dataset.memberSeq
	};
	(0, a.createRoot)(d).render(/* @__PURE__ */ (0, o.jsx)(u, {
		id: e,
		toss: t
	}));
}
//#endregion
