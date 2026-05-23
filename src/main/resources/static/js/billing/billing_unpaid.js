/* ================================================================
   billing_unpaid.js — 관리자 미납 세대 관리
================================================================ */

'use strict';

const YEARS  = [2026,2025,2024,2023,2022,2021,2020,2019,2018];
const MONTHS = ['1월','2월','3월','4월','5월','6월','7월','8월',
                '9월','10월','11월','12월'];

let selYear        = new Date().getFullYear();
let selMonth       = null;
let selDong        = null;
let selFilter      = 'unpaid';
let currentPage    = 0;
let openPanel      = null;
let pendingBilling = null;
let allItems       = [];       // 현재 페이지 데이터 (체크박스용)
let dongList       = [];       // 동 목록 (API 응답에서 수집)
let sortKey        = null;
let sortDir        = 1;

/* ================================================================
   초기화
================================================================ */
document.addEventListener('DOMContentLoaded', () => {
    fetchDongList();
    fetchUnpaidList();
});

/* ================================================================
   필터 패널
================================================================ */
function onFilterChange() {
    const year   = document.getElementById('selYear').value;
    const month  = document.getElementById('selMonth').value;
    const dong   = document.getElementById('selDong').value;
    const filter = document.getElementById('selFilter').value;

    selYear   = year   ? parseInt(year)   : null;
    selMonth  = month  ? parseInt(month)  : null;
    selDong   = dong   || null;
    selFilter = filter || 'unpaid';

    currentPage = 0;
    fetchUnpaidList();
}

function updateDongOptions() {
    const dongs = [...new Set(allItems.map(i => i.dong).filter(Boolean))].sort();
    const sel = document.getElementById('selDong');
    const cur = sel.value;
    sel.innerHTML = '<option value="">전체 동</option>'
        + dongs.map(d => `<option value="${d}">${d}</option>`).join('');
    sel.value = cur;
}

// 동 목록 전체 조회 (필터와 무관하게 전체 동 목록 확보)
async function fetchDongList() {
    try {
        const params = new URLSearchParams();
        if (selYear) params.set('year', selYear);
        params.set('size', 500);
        const res  = await fetch(`${CONTEXT_PATH}/api/billing/admin/list?${params}`);
        const data = await res.json();
        // 기존 dongList 관련 코드 삭제 후
        if (!selDong && allItems.length > 0) {
            updateDongOptions();
        }
    } catch (err) {
        console.warn('동 목록 조회 실패', err);
    }
}

/* ================================================================
   API 조회
================================================================ */

async function fetchUnpaidList() {
    const params = new URLSearchParams();

    // ★ 연도/월 파라미터 조합 처리
    if (selYear && selMonth) {
        // 연도 + 월 둘 다 선택 → month (예: "2026-01")
        params.set('month', `${selYear}-${String(selMonth).padStart(2,'0')}`);
    } else if (selYear && !selMonth) {
        // 연도만 선택 → year
        params.set('year', selYear);
    } else if (!selYear && selMonth) {
        // 전체 연도 + 월만 선택 → monthOnly (예: "01")
        // 백엔드 findAllWithAdminFilter의 :monthOnly 파라미터 사용
        params.set('monthOnly', String(selMonth).padStart(2,'0'));
    }
    // 둘 다 null → 파라미터 없음 (전체 조회)

    if (selDong)  params.set('dong', selDong);
    if (selFilter === 'unpaid') params.set('status',      'UNPAID');
    if (selFilter === 'paid')   params.set('status',      'PAID');
    if (selFilter === 'long')   params.set('overdueOnly', 'true');

    params.set('page', currentPage);
    params.set('size', 20);

    try {
        const res  = await fetch(`${CONTEXT_PATH}/api/billing/admin/unpaid?${params}`);
        const data = await res.json();

        allItems = data.content || [];

        if (!selDong) {
            const newDongs = [...new Set(allItems.map(i => i.dong).filter(Boolean))].sort();
            if (newDongs.length > 0) dongList = [...new Set([...dongList, ...newDongs])].sort();
        }

        clearAllChecks();
        renderTableBody(allItems);
        renderPagination(data.number, data.totalPages);
        refreshStats();

    } catch (err) {
        console.error('미납 목록 조회 실패', err);
    }
}

/* ================================================================
   테이블 렌더링
================================================================ */
function renderTableBody(items) {

    // ★ 정렬
    if (sortKey && items && items.length > 0) {
        items = [...items].sort((a, b) => {
            const av = a[sortKey] ?? '';
            const bv = b[sortKey] ?? '';
            if (av < bv) return -sortDir;
            if (av > bv) return  sortDir;
            return 0;
        });
    }

    const tbody = document.getElementById('billingTableBody');

    if (!items || items.length === 0) {
        tbody.innerHTML = `<tr><td colspan="9" class="empty-state">조회된 세대가 없습니다.</td></tr>`;
        return;
    }

    tbody.innerHTML = items.map((item, idx) => {
        const isUnpaid = item.status === 'UNPAID';
        const num      = currentPage * 20 + idx + 1;

        const checkbox = isUnpaid
            ? `<input type="checkbox" class="row-check"
                data-billing-id="${item.billingId}"
                data-unit="${item.unit || '—'}"
                onchange="onCheckChange()">`
            : '';

        const badge = isUnpaid
            ? `<span class="badge badge-unpaid">UNPAID</span>`
            : `<span class="badge badge-paid">PAID</span>`;

        const action = isUnpaid
            ? `<button class="btn-process"
                data-billing-id="${item.billingId}"
                data-unit="${item.unit || '—'}"
                data-month="${item.billingMonth}"
                onclick="openConfirm(this)">납부완료 처리</button>`
            : `<span class="btn-process done-txt">처리완료</span>`;

        return `
        <tr class="${isUnpaid ? '' : 'done'}">
            <td>${checkbox}</td>
            <td>${num}</td>
            <td>${item.unit || '—'}</td>
            <td>${item.residentName || '—'}</td>
            <td>${item.billingMonth || '—'}</td>
            <td>${Number(item.totalAmount).toLocaleString()}원</td>
            <td>${item.dueDate ? item.dueDate.replace(/-/g,'.') : '—'}</td>
            <td>${badge}</td>
            <td>${action}</td>
        </tr>`;
    }).join('');
}

function renderPagination(cur, total) {
    const wrap = document.getElementById('paginationWrap');
    if (!wrap) return;
    if (total <= 1) { wrap.innerHTML = ''; return; }

    wrap.innerHTML =
        `<button class="page-btn${cur===0?' disabled':''}"
            onclick="${cur>0?`goPage(${cur-1})`:''}">&lt;</button>`
        + Array.from({ length: total }, (_,i) =>
            `<button class="page-btn${i===cur?' active':''}" onclick="goPage(${i})">${i+1}</button>`
        ).join('')
        + `<button class="page-btn${cur===total-1?' disabled':''}"
            onclick="${cur<total-1?`goPage(${cur+1})`:''}">&gt;</button>`;
}

function goPage(page) { currentPage = page; fetchUnpaidList(); }

/* ================================================================
   체크박스 — 전체선택 / 개별 / 액션바
================================================================ */
function toggleAllChecks(masterCb) {
    document.querySelectorAll('.row-check').forEach(cb => {
        cb.checked = masterCb.checked;
    });
    onCheckChange();
}

function onCheckChange() {
    const checked  = getCheckedIds();
    const allBoxes = document.querySelectorAll('.row-check');
    const master   = document.getElementById('checkAll');

    // 전체선택 체크박스 상태 동기화
    if (master) {
        master.checked       = allBoxes.length > 0 && checked.length === allBoxes.length;
        master.indeterminate = checked.length > 0 && checked.length < allBoxes.length;
    }

    // 액션바 표시/숨김
    const bar = document.getElementById('bulkActionBar');
    if (bar) {
        bar.style.display = checked.length > 0 ? 'flex' : 'none';
        document.getElementById('bulkCount').textContent = `${checked.length}건 선택됨`;
    }
}

function getCheckedIds() {
    return [...document.querySelectorAll('.row-check:checked')]
        .map(cb => Number(cb.dataset.billingId));
}

function clearAllChecks() {
    document.querySelectorAll('.row-check').forEach(cb => cb.checked = false);
    const master = document.getElementById('checkAll');
    if (master) { master.checked = false; master.indeterminate = false; }
    const bar = document.getElementById('bulkActionBar');
    if (bar) bar.style.display = 'none';
}

/* ================================================================
   단건 납부완료 처리
================================================================ */
function openConfirm(btn) {
    pendingBilling = {
        billingId: btn.dataset.billingId,
        unit:      btn.dataset.unit,
        month:     btn.dataset.month,
    };
    document.getElementById('confirmUnit').textContent  = pendingBilling.unit;
    document.getElementById('confirmMonth').textContent =
        pendingBilling.month.replace('-','년 ') + '월 관리비';
    document.getElementById('confirmOverlay').style.display = 'flex';
}

function closeConfirm() {
    document.getElementById('confirmOverlay').style.display = 'none';
    pendingBilling = null;
}

async function processPayment() {
    if (!pendingBilling) return;
    const { billingId } = pendingBilling;
    closeConfirm();

    try {
        const res = await fetch(
            `${CONTEXT_PATH}/api/billing/admin/${billingId}/pay?adminId=1`,
            { method: 'POST', headers: { [CSRF_HEADER]: CSRF_TOKEN } }
        );
        if (!res.ok) throw new Error('처리 실패');

        showToast('납부완료 처리되었습니다.');
        fetchUnpaidList();   // ★ DOM 수정 대신 목록 전체 재조회
        refreshStats();

    } catch (err) {
        console.error('납부완료 처리 실패', err);
        alert('처리 중 오류가 발생했습니다.');
    }
}

/* ================================================================
   일괄 납부완료 처리
================================================================ */
function openBulkConfirm() {
    const ids = getCheckedIds();
    if (ids.length === 0) return;

    document.getElementById('bulkConfirmMsg').textContent =
        `${ids.length}세대를 납부완료 처리하시겠습니까?`;
    document.getElementById('bulkConfirmOverlay').style.display = 'flex';
}

function closeBulkConfirm() {
    document.getElementById('bulkConfirmOverlay').style.display = 'none';
}

async function processBulkPayment() {
    const ids = getCheckedIds();
    if (ids.length === 0) return;
    closeBulkConfirm();

    try {
        const res = await fetch(
            `${CONTEXT_PATH}/api/billing/admin/pay/bulk?adminId=1`,
            {
                method:  'POST',
                headers: { 'Content-Type': 'application/json', [CSRF_HEADER]: CSRF_TOKEN },
                body:    JSON.stringify(ids),
            }
        );
        if (!res.ok) throw new Error('처리 실패');
        const data = await res.json();

        showToast(`${data.processed}세대 납부완료 처리되었습니다.`);
        clearAllChecks();
        fetchUnpaidList();   // ★ DOM 수정 대신 목록 전체 재조회
        refreshStats();

    } catch (err) {
        console.error('일괄 처리 실패', err);
        alert('처리 중 오류가 발생했습니다.');
    }
}

/* ================================================================
   통계 카드 갱신
================================================================ */
async function refreshStats() {
    const params = new URLSearchParams();
    if (selYear && selMonth) {
        params.set('month', `${selYear}-${String(selMonth).padStart(2,'0')}`);
    } else if (selYear) {
        params.set('year', selYear);
    }
    if (selDong) params.set('dong', selDong);

    try {
        const res  = await fetch(`${CONTEXT_PATH}/api/billing/admin/stats/dashboard?${params}`);
        const data = await res.json();

        // 윗줄: 필터 기준 통계
        const el = id => document.getElementById(id);
        if (el('statTotal'))   el('statTotal').textContent   = data.totalHouseholds ?? '—';
        if (el('statPaid'))    el('statPaid').textContent    = data.paidCount       ?? '—';
        if (el('statUnpaid'))  el('statUnpaid').textContent  = data.unpaidCount     ?? '—';
        if (el('statRate'))    el('statRate').textContent    =
            data.paidRate != null ? data.paidRate.toFixed(1) + '%' : '—';

        // 필터 라벨 갱신
        const labelParts = [];
        if (selYear)  labelParts.push(selYear + '년');
        if (selMonth) labelParts.push(selMonth + '월');
        if (selDong)  labelParts.push(selDong);
        const label = labelParts.length > 0
            ? labelParts.join(' · ') + ' 기준'
            : '전체 기간 기준';
        if (el('statFilterLabel')) el('statFilterLabel').textContent = label;

        // 아랫줄: 전체 고정 통계
        if (el('statGlobalBillings'))   el('statGlobalBillings').textContent   =
            (data.globalUnpaidBillings   ?? '—') + '건';
        if (el('statGlobalHouseholds'))  el('statGlobalHouseholds').textContent  =
            (data.globalUnpaidHouseholds  ?? '—') + '세대';
        if (el('statGlobalOverdue'))     el('statGlobalOverdue').textContent     =
            (data.globalOverdueHouseholds ?? '—') + '세대';

    } catch (err) {
        console.error('통계 갱신 실패', err);
    }
}

function toggleGlobalStats() {
    const wrap = document.getElementById('globalStatsWrap');
    const icon = document.getElementById('globalStatsIcon');
    const isHidden = wrap.style.display === 'none';
    wrap.style.display = isHidden ? 'block' : 'none';
    icon.textContent   = isHidden ? '▲' : '▼';
}

/* ================================================================
   토스트
================================================================ */
function showToast(msg) {
    const t = document.createElement('div');
    t.textContent = msg;
    t.style.cssText = `
        position:fixed; top:20px; right:20px;
        background:#2c8a3e; color:white;
        padding:12px 20px; border-radius:6px;
        box-shadow:0 2px 8px rgba(0,0,0,.15);
        z-index:10000; font-size:13px;
    `;
    document.body.appendChild(t);
    setTimeout(() => t.remove(), 3000);
}

function sortBy(key) {
    if (sortKey === key) sortDir *= -1;
    else { sortKey = key; sortDir = 1; }
    renderTableBody(allItems);
}

