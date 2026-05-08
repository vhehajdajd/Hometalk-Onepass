/**
 * HomeTalk OnePass — Bell Icon + Dropdown + Toast (All-in-one)
 * path: /static/notification/js/notification-bell.js
 *
 * 헤더 fragment(headerCurrentMenuView)에 다음 마크업 필요:
 *   <div class="noti-bell-wrap" style="position:relative">
 *       <a class="noti-bell" id="noti-bell-trigger">
 *           <span class="noti-bell__badge" id="noti-bell-badge"></span>
 *       </a>
 *       <div class="noti-dropdown" id="noti-dropdown">
 *           <div class="noti-dropdown__header">
 *               <span class="noti-dropdown__title">알림</span>
 *               <button class="noti-dropdown__action" id="noti-mark-all">전체 읽음</button>
 *           </div>
 *           <div class="noti-dropdown__body" id="noti-dropdown-body">
 *               <div class="noti-loading">불러오는 중...</div>
 *           </div>
 *           <div class="noti-dropdown__footer">
 *               <button class="noti-dropdown__more" id="noti-load-more">더보기</button>
 *           </div>
 *       </div>
 *   </div>
 *
 * 그리고 다음 스크립트 포함:
 *   <script th:src="@{/notification/js/notification-sse.js}"></script>
 *   <script th:src="@{/notification/js/notification-bell.js}"></script>
 */
(function () {
    'use strict';

    const CTX = window.NOTI_CTX || '';

    const state = {
        page: 0,
        size: 3,
        last: false,
        loading: false,
        items: []
    }
    // 더보기 초기 숨김 (DOMContentLoaded 안에 추가)
    const loadMoreBtn = document.getElementById('noti-load-more');
    if (loadMoreBtn) loadMoreBtn.style.display = 'none';

    // markAllRead 수정
    async function markAllRead() {
        await fetch(`${CTX}/api/notification/read-all`, {
            method: 'POST', credentials: 'include'
        });
        await fetch(`${CTX}/api/notification/delete-all`, {
            method: 'DELETE', credentials: 'include'
        });
    }

    // ─────────────────── API ───────────────────

    async function fetchUnreadCount() {
        try {
            const res = await fetch(`${CTX}/api/notification/unread-count`, {
                credentials: 'include'
            });
            if (!res.ok) return 0;
            const data = await res.json();
            return data.count ?? 0;
        } catch (e) {
            console.error('[Bell] count fetch failed:', e);
            return 0;
        }
    }

    async function fetchList(page) {
        const url = `${CTX}/api/notification/list?page=${page}&size=${state.size}`;
        const res = await fetch(url, { credentials: 'include' });
        if (!res.ok) throw new Error('fetch failed');
        return res.json();
    }

    async function markRead(id) {
        await fetch(`${CTX}/api/notification/${id}/read`, {
            method: 'POST', credentials: 'include'
        });
    }

    // 전체삭제 버튼 → 읽음+삭제
    async function markAllRead() {
        await fetch(`${CTX}/api/notification/read-all`, {
            method: 'POST', credentials: 'include'
        });
        await fetch(`${CTX}/api/notification/delete-all`, {
            method: 'DELETE', credentials: 'include'
        });
    }

    // ─────────────────── 뱃지 ───────────────────

    function renderBadge(count) {
        const badge = document.getElementById('noti-bell-badge');
        if (!badge) return;

        if (count > 0) {
            badge.textContent = count > 99 ? '99+' : count;
            badge.style.display = 'flex';     // 추가: 숫자가 있을 때만 노출
            badge.classList.add('is-active');
        } else {
            badge.style.display = 'none';     // 추가: 숫자가 없으면 숨김
            badge.classList.remove('is-active');
        }
    }



    // ─────────────────── 드롭다운 토글 ───────────────────

    function toggleDropdown(force) {
        const dropdown = document.getElementById('noti-dropdown');
        if (!dropdown) return;

        const isOpen = dropdown.classList.contains('is-open');
        const willOpen = (force === undefined) ? !isOpen : force;

        if (willOpen) {
            dropdown.classList.add('is-open');
            loadList(0, false);   // 열 때마다 최신 데이터 fetch
        } else {
            dropdown.classList.remove('is-open');
        }
    }

    // ─────────────────── 목록 렌더링 ───────────────────

async function loadList(page, append, size) {
    if (state.loading) return;
    state.loading = true;

    const body   = document.getElementById('noti-dropdown-body');
    const footer = document.getElementById('noti-load-more');

    if (!append && body) {
        body.innerHTML = '<div class="noti-loading">불러오는 중...</div>';
    }

    try {
        const actualSize = size || state.size;
        const url = `${CTX}/api/notification/list?page=${page}&size=${actualSize}`;
        const res = await fetch(url, { credentials: 'include' });
        if (!res.ok) throw new Error('fetch failed');
        const data = await res.json();

        state.last = data.last;
        state.page = page;

        if (!append) state.items = [];
        state.items = state.items.concat(data.content);

        renderItems(append);

        if (footer) {
            footer.style.display = data.last ? 'none' : 'inline-block';
        }
    } catch (e) {
        console.error('[Bell] list load failed:', e);
        if (body) body.innerHTML = '<div class="noti-empty">불러오기 실패</div>';
    } finally {
        state.loading = false;
    }
}

    function renderItems(append) {
        const body = document.getElementById('noti-dropdown-body');
        if (!body) return;

        if (!append) body.innerHTML = '';

        if (state.items.length === 0) {
            body.innerHTML = `
                <div class="noti-empty">
                    <div class="noti-empty__icon">📭</div>
                    <div>알림이 없습니다.</div>
                </div>
            `;
            return;
        }

        const itemsToRender = append
            ? state.items.slice(state.page * state.size)
            : state.items;

        itemsToRender.forEach(item => {
            body.appendChild(createItem(item));
        });
    }

    function createItem(item) {
        const el = document.createElement('div');
        el.className = `noti-item ${item.isRead ? 'is-read' : 'is-unread'}`;
        el.dataset.id = item.id;

        el.innerHTML = `
            <div class="noti-item__icon">${item.icon}</div>
            <div class="noti-item__body">
                <div class="noti-item__meta">
                    <span class="noti-item__category">${escapeHtml(item.category)}</span>
                    <span class="noti-item__time">${formatTime(item.createdAt)}</span>
                </div>
                <div class="noti-item__title">${escapeHtml(item.title)}</div>
                <div class="noti-item__message">${escapeHtml(item.message)}</div>
            </div>
            ${item.isRead ? '' : '<span class="noti-item__dot"></span>'}
        `;

        el.addEventListener('click', async (e) => {
            e.stopPropagation();
            await handleClick(item, el);
        });
        return el;
    }

    async function handleClick(item, element) {
        if (!item.isRead) {
            try {
                await markRead(item.id);
                item.isRead = true;
                element.classList.remove('is-unread');
                element.classList.add('is-read');
                element.querySelector('.noti-item__dot')?.remove();
                renderBadge(await fetchUnreadCount());
            } catch (e) {
                console.error('[Bell] mark read failed:', e);
            }
        }

        if (item.link) {
            window.location.href = CTX + item.link;
        }
    }

    // ─────────────────── 토스트 ───────────────────

    function showToast(data) {
        let container = document.querySelector('.noti-toast-container');
        if (!container) {
            container = document.createElement('div');
            container.className = 'noti-toast-container';
            document.body.appendChild(container);
        }

        const toast = document.createElement('div');
        toast.className = 'noti-toast';
        toast.innerHTML = `
            <div class="noti-toast__title">${data.icon || '🔔'} ${escapeHtml(data.title)}</div>
            <div class="noti-toast__message">${escapeHtml(data.message)}</div>
        `;
        toast.addEventListener('click', () => {
            if (data.link) window.location.href = CTX + data.link;
        });

        container.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = '0';
            setTimeout(() => toast.remove(), 300);
        }, 7000);
    }

    // ─────────────────── 헬퍼 ───────────────────

    function formatTime(iso) {
        if (!iso) return '';
        const date = new Date(iso);
        const now  = new Date();
        const diff = (now - date) / 1000;

        if (diff < 60)     return '방금 전';
        if (diff < 3600)   return `${Math.floor(diff / 60)}분 전`;
        if (diff < 86400)  return `${Math.floor(diff / 3600)}시간 전`;
        if (diff < 604800) return `${Math.floor(diff / 86400)}일 전`;

        return date.toLocaleDateString('ko-KR', {
            year: 'numeric', month: '2-digit', day: '2-digit'
        });
    }

    function escapeHtml(str) {
        if (!str) return '';
        return String(str).replace(/[&<>"']/g, (c) => ({
            '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
        }[c]));
    }

    // ─────────────────── 초기화 ───────────────────

    document.addEventListener('DOMContentLoaded', async () => {
        // 1) 페이지 로드 시 미읽음 수 갱신
        renderBadge(await fetchUnreadCount());

        // ✅ 더보기 버튼 초기 숨김
        const loadMoreBtn = document.getElementById('noti-load-more');
        if (loadMoreBtn) loadMoreBtn.style.display = 'none';

        // 2) 벨 클릭 → 드롭다운 토글
        document.getElementById('noti-bell-trigger')?.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            toggleDropdown();
        });

        // 3) 드롭다운 외부 클릭 시 닫기
        document.addEventListener('click', (e) => {
            const dropdown = document.getElementById('noti-dropdown');
            const trigger  = document.getElementById('noti-bell-trigger');
            if (!dropdown || !trigger) return;

            if (!dropdown.contains(e.target) && !trigger.contains(e.target)) {
                toggleDropdown(false);
            }
        });

        // 드롭다운 내부 클릭은 패널 유지
        document.getElementById('noti-dropdown')?.addEventListener('click', (e) => {
            e.stopPropagation();
        });

        // 4) 더보기 클릭 시 전체 로드
        document.getElementById('noti-load-more')?.addEventListener('click', () => {
            loadList(0, false, 100); // size 100으로 전체 로드
        });

        // 5) 전체 읽음
        document.getElementById('noti-mark-all')?.addEventListener('click', async () => {
            await markAllRead();
            await loadList(0, false);
            renderBadge(await fetchUnreadCount());
        });

        // 6) SSE — 새 알림 수신 시 토스트 + 뱃지 갱신 + 드롭다운 열려있으면 새로고침
        new NotificationSSE({
            onMessage: async (data) => {
                showToast(data);
                renderBadge(await fetchUnreadCount());

                const dropdown = document.getElementById('noti-dropdown');
                if (dropdown?.classList.contains('is-open')) {
                    await loadList(0, false);
                }
            },
            onConnect: async () => {
                renderBadge(await fetchUnreadCount());
            }
        });

        // 7) 다른 탭에서 변경된 경우 동기화
        window.addEventListener('focus', async () => {
            renderBadge(await fetchUnreadCount());
        });


    });
})();