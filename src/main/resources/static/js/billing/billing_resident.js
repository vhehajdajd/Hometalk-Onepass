/* ================================================================
   billing_resident.js — 입주민 관리비 페이지
================================================================ */
'use strict';

const YEARS_R  = [2026,2025,2024,2023,2022,2021,2020,2019,2018];
const MONTHS_R = ['1월','2월','3월','4월','5월','6월','7월','8월',
                  '9월','10월','11월','12월'];

let rSelYear   = new Date().getFullYear();
let rSelMonth  = null;
let rSelStatus = null;   // null=전체, 'UNPAID', 'PAID'
let rPage      = 0;
let rHasMore   = false;
let rOpenPanel = null;

/* ================================================================
   초기화
================================================================ */
document.addEventListener('DOMContentLoaded', () => {
    // 초기 데이터는 Thymeleaf 서버사이드로 이미 렌더링됨
    // 필터 변경 시 API 재조회
});

/* ================================================================
   필터 드롭다운
================================================================ */
function onFilterChange() {
    const year   = document.getElementById('selYear').value;
    const month  = document.getElementById('selMonth').value;
    const status = document.getElementById('selStatus').value;

    rSelYear   = year   ? parseInt(year)   : null;
    rSelMonth  = month  ? parseInt(month)  : null;
    rSelStatus = status || null;

    fetchBillingList(true);
}
/* ================================================================
   API 조회
================================================================ */
async function fetchBillingList(reset = false) {
    if (reset) rPage = 0;

    const params = new URLSearchParams();
    params.set('householdId', HOUSEHOLD_ID);

    if (rSelYear && !rSelMonth) params.set('year', rSelYear);
    if (rSelMonth) params.set('month', `${rSelYear}-${String(rSelMonth).padStart(2,'0')}`);
    if (rSelStatus) params.set('status', rSelStatus);
    params.set('size', 12);
    params.set('page', rPage);

    try {
        const res  = await fetch(`${CONTEXT_PATH}/api/billing/resident/list?${params}`);
        const data = await res.json();

        renderBillingList(data.content || [], reset);
        rHasMore = !data.last;
        document.getElementById('loadMoreWrap').style.display = rHasMore ? '' : 'none';

    } catch (err) {
        console.error('관리비 목록 조회 실패', err);
    }
}

/* ================================================================
   목록 렌더링
================================================================ */
function renderBillingList(items, reset) {
    const list = document.getElementById('billingList');

    if (reset) list.innerHTML = '';

    if (items.length === 0 && reset) {
        list.innerHTML = '<div class="bp-empty">조회된 관리비 내역이 없습니다.</div>';
        return;
    }

    const now = new Date();

    items.forEach(b => {
        const isUnpaid = b.status === 'UNPAID';
        const dueDate  = b.dueDate ? new Date(b.dueDate) : null;
        const isOverdue = isUnpaid && dueDate && now > dueDate;

        const item = document.createElement('div');
        item.className = `bp-item${isOverdue ? ' overdue' : ''}`;
        item.innerHTML = `
            <div class="bp-item-left">
                <div class="bp-item-month">${b.billingMonth} 관리비</div>
                <div class="bp-item-due ${isUnpaid ? '' : 'paid-text'}">
                    ${isUnpaid
                        ? '납부기한 ' + (b.dueDate ? b.dueDate.replace(/-/g,'.') : '—')
                        : '납부 완료'}
                </div>
            </div>
            <div class="bp-item-mid">
                <span class="bp-badge ${isUnpaid ? 'unpaid' : 'paid'}">${b.status}</span>
                <span class="bp-amount ${isUnpaid ? 'unpaid' : ''}">
                    ${Number(b.totalAmount).toLocaleString()}원
                </span>
            </div>
            <button class="bp-btn-view ${isUnpaid ? 'primary' : 'secondary'}"
                    onclick="openModal(${b.billingId})">고지서 보기</button>`;
        list.appendChild(item);
    });
}

function loadMore() {
    rPage++;
    fetchBillingList(false);
}

/* ================================================================
   고지서 모달
================================================================ */
async function openModal(billingId) {
    try {
        const res  = await fetch(`${CONTEXT_PATH}/api/billing/${billingId}/detail`);
        const data = await res.json();

        const isUnpaid  = data.status === 'UNPAID';
        const now       = new Date();
        const due       = data.dueDate ? new Date(data.dueDate) : null;
        const isOverdue = isUnpaid && due && now > due;

        // 헤더
        document.getElementById('modalHeaderTitle').textContent =
            `${data.billingMonth} · ${data.dongHo}`;
        document.getElementById('modalPeriod').textContent =
            `부과월: ${data.billingMonth} · 납부기한: ${data.dueDate ? data.dueDate.replace(/-/g,'.') : '—'}`;

        // 항목 목록
        document.getElementById('modalRows').innerHTML = (data.items || []).length
            ? (data.items || []).map(d =>
                `<div class="bill-row">
                    <span>${d.itemName}</span>
                    <span>${Number(d.itemAmount).toLocaleString()}원</span>
                </div>`).join('')
            : '<div style="color:#aaa;text-align:center;padding:16px 0;">항목 정보 없음</div>';

        // 합계
        document.getElementById('modalTotal').textContent =
            Number(data.totalAmount).toLocaleString() + '원';

        // 납부 상태 박스
        const statusBox = document.getElementById('modalStatusBox');
        if (isUnpaid && isOverdue) {
            statusBox.className = 'bm-status-box past';
            statusBox.innerHTML = `<div class="bm-status-title">납부기한이 지났습니다.</div>
                                   <div class="bm-status-sub">관리사무소에 문의해 주세요. ☎️ 02-888-9999</div>`;
        } else if (isUnpaid) {
            statusBox.className = 'bm-status-box upcoming';
            statusBox.innerHTML = `<div class="bm-status-title">납부기한</div>
                                   <div class="bm-status-sub">${data.dueDate ? data.dueDate.replace(/-/g,'.') : '—'}</div>`;
        } else {
            statusBox.className = 'bm-status-box done';
            statusBox.innerHTML = `<div class="bm-status-title">납부 완료되었습니다.</div>`;
        }

        // ★ 모달 표시
        document.getElementById('modalOverlay').style.display = 'flex';

    } catch (err) {
        console.error('고지서 조회 실패', err);
    }
}

function closeModal(e) {
    if (e && e.target !== document.getElementById('modalOverlay')) return;
    closeModalBtn();
}

function closeModalBtn() {
    document.getElementById('modalOverlay').style.display = 'none';
}