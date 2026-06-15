/*
 * 알림 헤더 벨: 안읽음 배지 + 드롭다운 목록 + SSE 실시간 푸시.
 * 헤더에 #notif-bell 이 있을 때(=로그인)만 동작한다. 비로그인 페이지에선 아무 것도 하지 않는다.
 * 백엔드: GET /api/notifications, /unread-count, /subscribe(SSE), POST /{seq}/read, /read-all
 */
(function () {
    document.addEventListener('DOMContentLoaded', function () {
        const bell = document.getElementById('notif-bell');
        if (!bell) return; // 비로그인

        const badge = document.getElementById('notif-badge');
        const dropdown = document.getElementById('notif-dropdown');
        const listEl = document.getElementById('notif-list');
        const readAllBtn = document.getElementById('notif-read-all');

        let unread = 0;

        function renderBadge() {
            if (unread > 0) {
                badge.textContent = unread > 99 ? '99+' : String(unread);
                badge.classList.remove('hidden');
            } else {
                badge.classList.add('hidden');
            }
        }

        function fetchUnreadCount() {
            fetch('/api/notifications/unread-count')
                .then(r => r.ok ? r.json() : null)
                .then(data => {
                    if (data) { unread = data.count || 0; renderBadge(); }
                })
                .catch(() => {});
        }

        function timeAgo(iso) {
            if (!iso) return '';
            const diff = (Date.now() - new Date(iso).getTime()) / 1000;
            if (diff < 60) return '방금';
            if (diff < 3600) return Math.floor(diff / 60) + '분 전';
            if (diff < 86400) return Math.floor(diff / 3600) + '시간 전';
            return Math.floor(diff / 86400) + '일 전';
        }

        function renderList(items) {
            if (!items || items.length === 0) {
                listEl.innerHTML = '<li class="px-4 py-6 text-center text-gray-400">알림이 없습니다.</li>';
                return;
            }
            listEl.innerHTML = items.map(n => `
                <li class="notif-item px-4 py-3 cursor-pointer hover:bg-gray-50 ${n.read ? '' : 'bg-amber-50'}" data-seq="${n.seq}" data-read="${n.read}">
                    <div class="flex items-start gap-2">
                        ${n.read ? '' : '<span class="mt-1 w-1.5 h-1.5 rounded-full bg-amber-500 shrink-0"></span>'}
                        <div class="flex-1 min-w-0">
                            <div class="font-bold text-gray-800 truncate">${escapeHtml(n.title)}</div>
                            <div class="text-gray-500 mt-0.5 leading-snug">${escapeHtml(n.content)}</div>
                            <div class="text-[10px] text-gray-400 mt-1">${timeAgo(n.createdAt)}</div>
                        </div>
                    </div>
                </li>`).join('');
        }

        function escapeHtml(s) {
            return (s || '').replace(/[&<>"']/g, c =>
                ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
        }

        function loadList() {
            listEl.innerHTML = '<li class="px-4 py-6 text-center text-gray-400">불러오는 중...</li>';
            fetch('/api/notifications')
                .then(r => r.ok ? r.json() : [])
                .then(renderList)
                .catch(() => { listEl.innerHTML = '<li class="px-4 py-6 text-center text-gray-400">불러오지 못했습니다.</li>'; });
        }

        function markRead(seq) {
            return fetch('/api/notifications/' + seq + '/read', { method: 'POST' });
        }

        // 벨 클릭 → 드롭다운 토글 + 목록 로드
        bell.addEventListener('click', function (e) {
            e.stopPropagation();
            const willOpen = dropdown.classList.contains('hidden');
            dropdown.classList.toggle('hidden');
            if (willOpen) loadList();
        });

        // 항목 클릭 → 읽음 처리(안읽음이면 배지 -1)
        listEl.addEventListener('click', function (e) {
            const item = e.target.closest('.notif-item');
            if (!item) return;
            const seq = item.getAttribute('data-seq');
            const wasUnread = item.getAttribute('data-read') === 'false';
            if (wasUnread) {
                markRead(seq).then(() => {
                    item.setAttribute('data-read', 'true');
                    item.classList.remove('bg-amber-50');
                    unread = Math.max(0, unread - 1);
                    renderBadge();
                }).catch(() => {});
            }
        });

        // 모두 읽음
        if (readAllBtn) {
            readAllBtn.addEventListener('click', function (e) {
                e.stopPropagation();
                fetch('/api/notifications/read-all', { method: 'POST' })
                    .then(() => { unread = 0; renderBadge(); loadList(); })
                    .catch(() => {});
            });
        }

        // 드롭다운 바깥 클릭 시 닫기
        document.addEventListener('click', function (e) {
            if (!dropdown.classList.contains('hidden')
                && !dropdown.contains(e.target) && !bell.contains(e.target)) {
                dropdown.classList.add('hidden');
            }
        });

        // SSE 실시간 구독: 새 알림 도착 시 배지 +1, 드롭다운 열려 있으면 목록 갱신
        function connectSse() {
            if (typeof EventSource === 'undefined') return;
            const es = new EventSource('/api/notifications/subscribe');
            es.addEventListener('notification', function () {
                unread += 1;
                renderBadge();
                if (!dropdown.classList.contains('hidden')) loadList();
            });
            es.onerror = function () {
                // 브라우저가 자동 재연결한다. 과도한 로그 방지를 위해 조용히 둔다.
            };
        }

        fetchUnreadCount();
        connectSse();
    });
})();
