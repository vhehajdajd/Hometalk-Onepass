/* ================================================================
   billing_monthly.js  —  Tab 2: 월별 관리
================================================================ */
'use strict';

const YEARS  = [2026,2025,2024,2023,2022,2021,2020,2019,2018];
const MONTHS_KR = ['1월','2월','3월','4월','5월','6월','7월','8월',
                   '9월','10월','11월','12월'];

let billingRows        = [];
let selYear            = new Date().getFullYear();
let selMonth           = null;
let selDong            = null;
let openPanel          = null;
let pendingDeleteMonth = null;
let sortKey            = null;
let sortDir            = 1;

/* ================================================================
   초기화
================================================================ */
document.addEventListener('DOMContentLoaded', () => {
    fetchList();
    document.addEventListener('click', e => {
        if (!e.target.closest('.panel-wrap')) closeAllPanels();
    });
});

/* ================================================================
   API 조회
================================================================ */
async function fetchList() {
    try {
        const params = new URLSearchParams();
        if (selYear)  params.set('year', selYear);
        if (selMonth && selYear)
            params.set('month', `${selYear}-${String(selMonth).padStart(2,'0')}`);
        if (selDong)  params.set('dong', selDong);
        params.set('size', 200);

        const res  = await fetch(`${CONTEXT_PATH}/api/billing/admin/list?${params}`);
        const data = await res.json();

        billingRows = (data.content || []).map((item, idx) => ({
            num:            idx + 1,
            billingId:      item.billingId,
            unit:           item.unit    || '—',
            dong:           item.dong    || '—',
            billingMonth:   item.billingMonth || '—',
            totalAmount:    item.totalAmount  || 0,
            status:         item.status  || '—',
            lastUploadType: item.lastUploadType || null,
        }));

        buildDongGrid();
        renderTable();
        updateDeleteLink();

    } catch (err) {
        console.warn('목록 조회 실패', err);
        document.getElementById('tableBody').innerHTML =
            `<tr><td colspan="7" style="text-align:center;padding:36px;color:#aaa;">데이터 조회에 실패했습니다.</td></tr>`;
    }
}

/* ================================================================
   테이블 렌더링
================================================================ */
function renderTable() {
      let rows = billingRows
        .filter(r => {
            if (!r.billingMonth || r.billingMonth === '—') return true;
            const [y, m] = r.billingMonth.split('-').map(Number);
            if (selYear  && y !== selYear)  return false;
            if (selMonth && m !== selMonth) return false;
            if (selDong  && r.dong !== selDong) return false;
            return true;
        })
        // ★ 정렬
        if (sortKey) {
            rows.sort((a, b) => {
                const av = a[sortKey] ?? '';
                const bv = b[sortKey] ?? '';
                if (av < bv) return -sortDir;
                if (av > bv) return  sortDir;
                return 0;
            });
        }
        rows = rows.map((r, i) => ({ ...r, num: i + 1 }));  // 정렬 후 num 할당

    document.getElementById('tableMeta').textContent  = `총 ${rows.length}건`;
    document.getElementById('tableSummary').textContent = '';

    document.getElementById('tableBody').innerHTML = rows.length
        ? rows.map(r => {
            const isUnpaid = r.status === 'UNPAID';
            const statusBadge = isUnpaid
                ? `<span class="badge badge-unpaid">UNPAID</span>`
                : `<span class="badge badge-paid">PAID</span>`;

            let upsertCell = '—';
            if (r.lastUploadType === 'INSERT')
                upsertCell = `<span class="badge badge-insert">INSERT</span>`;
            else if (r.lastUploadType === 'UPDATE')
                upsertCell = `<span class="badge badge-update">UPDATE</span>`;

            return `
            <tr>
                <td>${r.num}</td>
                <td>${r.unit}</td>
                <td>${r.billingMonth}</td>
                <td>${Number(r.totalAmount).toLocaleString()}원</td>
                <td>${statusBadge}</td>
                <td><button class="btn-preview" onclick="openPreview(${r.billingId})">미리보기 →</button></td>
                <td>${upsertCell}</td>
            </tr>`;
        }).join('')
        : `<tr><td colspan="7" style="text-align:center;padding:36px;color:#aaa;">조회된 데이터가 없습니다.</td></tr>`;
}

/* ================================================================
   필터
================================================================ */
function buildDongGrid() {
    const dongs = [...new Set(billingRows.map(r => r.dong).filter(d => d && d !== '—'))].sort();
    document.getElementById('dongGrid').innerHTML =
        `<button class="chip full${!selDong ? ' selected' : ''}" onclick="pickDong(null)">전체 동</button>`
        + dongs.map(d =>
            `<button class="chip${selDong === d ? ' selected' : ''}" onclick="pickDong('${d}')">${d}</button>`
        ).join('');
}

function buildYearGrid() {
    document.getElementById('yearGrid').innerHTML =
        `<button class="chip full${selYear === null ? ' selected' : ''}" onclick="pickYear(null)">전체</button>`
        + YEARS.map(y =>
            `<button class="chip${y === selYear ? ' selected' : ''}" onclick="pickYear(${y})">${y}년</button>`
        ).join('');
}

function buildMonthGrid() {
    document.getElementById('monthGrid').innerHTML =
        `<button class="chip full${selMonth === null ? ' selected' : ''}" onclick="pickMonth(null)">전체</button>`
        + MONTHS_KR.map((m, i) =>
            `<button class="chip${selMonth === i + 1 ? ' selected' : ''}" onclick="pickMonth(${i + 1})">${m}</button>`
        ).join('');
}

function togglePanel(name) {
    if (openPanel === name) { closeAllPanels(); return; }
    closeAllPanels();
    openPanel = name;
    if (name === 'year')  buildYearGrid();
    if (name === 'month') buildMonthGrid();
    const panelMap = { year:'panelYear', month:'panelMonth', dong:'panelDong' };
    const btnMap   = { year:'btnYear',   month:'btnMonth',   dong:'btnDong'  };
    document.getElementById(panelMap[name]).style.display = 'block';
    document.getElementById(btnMap[name]).classList.add('active');
}

function closeAllPanels() {
    ['panelYear','panelMonth','panelDong'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.style.display = 'none';
    });
    ['btnYear','btnMonth','btnDong'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.classList.remove('active');
    });
    openPanel = null;
}

function pickYear(y) {
    selYear = y; selDong = null;
    document.getElementById('lblYear').textContent = y ? y + '년' : '전체';
    document.getElementById('lblDong').textContent = '전체 동';
    closeAllPanels();
    fetchList();
}

function pickMonth(m) {
    selMonth = m; selDong = null;
    document.getElementById('lblMonth').textContent = m ? MONTHS_KR[m - 1] : '전체 월';
    document.getElementById('lblDong').textContent  = '전체 동';
    closeAllPanels();
    fetchList();
}

function pickDong(d) {
    selDong = d;
    document.getElementById('lblDong').textContent = d || '전체 동';
    closeAllPanels();
    buildDongGrid();
    renderTable();
    updateDeleteLink(); // 동 선택 시 삭제 링크 텍스트 갱신
}

/* 삭제 링크: 연도 + 월 선택 시만 표시 */
function updateDeleteLink() {
    const wrap = document.getElementById('deleteWrap');
    const link = document.querySelector('.delete-month-link');

    if (selYear && selMonth) {
        wrap.style.display = '';
        const month = `${selYear}-${String(selMonth).padStart(2,'0')}`;
        link.textContent = selDong
            ? `⚠ ${month} ${selDong} 데이터 삭제`
            : `⚠ ${month} 전체 데이터 삭제`;
    } else {
        wrap.style.display = 'none';
    }
}

/* ================================================================
   미리보기 모달
================================================================ */
async function openPreview(billingId) {
    try {
        const res  = await fetch(`${CONTEXT_PATH}/api/billing/${billingId}/detail`);
        const data = await res.json();

        document.getElementById('modalHeaderTitle').textContent =
            `고지서 미리보기 — ${data.dongHo} (${data.billingMonth})`;
        document.getElementById('modalPeriod').textContent =
            `부과월: ${data.billingMonth} · 납부기한: ${data.dueDate}`;
        document.getElementById('modalRows').innerHTML = (data.items || []).length
            ? (data.items || []).map(d =>
                `<div class="bill-row">
                    <span>${d.itemName}</span>
                    <span>${Number(d.itemAmount).toLocaleString()}원</span>
                </div>`).join('')
            : '<div style="font-size:13px;color:#aaa;text-align:center;padding:16px 0;">항목 정보 없음</div>';
        document.getElementById('modalTotal').textContent =
            Number(data.totalAmount).toLocaleString() + '원';
        document.getElementById('previewOverlay').style.display = 'flex';
    } catch (err) {
        console.error('미리보기 실패', err);
    }
}

function closePreview(e) {
    if (e && e.target !== document.getElementById('previewOverlay')) return;
    closePreviewBtn();
}
function closePreviewBtn() {
    document.getElementById('previewOverlay').style.display = 'none';
}

/* ================================================================
   월별 삭제 (2단계)
================================================================ */
function deleteCurrentMonth() {
    if (!selYear || !selMonth) {
        alert('삭제할 월을 먼저 필터로 선택해 주세요.\n(연도 + 월 둘 다 선택 필요)');
        return;
    }

    const month = `${selYear}-${String(selMonth).padStart(2,'0')}`;
    const count = billingRows.filter(r => {
        if (r.billingMonth !== month) return false;
        if (selDong && r.dong !== selDong) return false;
        return true;
    }).length;

    if (count === 0) {
        alert(`${selDong ? selDong + ' ' : ''}${month} 데이터가 없습니다.`);
        return;
    }

    pendingDeleteMonth = month;
    document.getElementById('deleteStep1Msg').textContent = selDong
        ? `${month} ${selDong} 데이터 ${count}건을 삭제하시겠습니까?`
        : `${month} 전체 데이터 ${count}건을 삭제하시겠습니까?`;
    document.getElementById('deleteStep1Overlay').style.display = 'flex';
}

function goToDeleteStep2() {
    document.getElementById('deleteStep1Overlay').style.display = 'none';
    document.getElementById('deleteStep2Msg').textContent =
        `${pendingDeleteMonth} 관리비 데이터를 영구 삭제합니다.`;
    document.getElementById('deleteStep2Overlay').style.display = 'flex';
}

function cancelDelete() {
    document.getElementById('deleteStep1Overlay').style.display = 'none';
    document.getElementById('deleteStep2Overlay').style.display = 'none';
    pendingDeleteMonth = null;
}

async function doDelete() {
    if (!pendingDeleteMonth) return;
    const btn = document.querySelector('#deleteStep2Overlay .btn-danger');
    if (btn) btn.disabled = true;

    // ★ dong 파라미터 추가
    const url = selDong
        ? `${CONTEXT_PATH}/api/billing/admin/month/${pendingDeleteMonth}?adminId=1&dong=${encodeURIComponent(selDong)}`
        : `${CONTEXT_PATH}/api/billing/admin/month/${pendingDeleteMonth}?adminId=1`;

    try {
        const res = await fetch(url, {
            method: 'DELETE',
            headers: { [CSRF_HEADER]: CSRF_TOKEN }
        });
        if (!res.ok) throw new Error('삭제 실패');
        const data = await res.json();

        document.getElementById('deleteStep2Overlay').style.display = 'none';
        showToast(`${pendingDeleteMonth}${selDong ? ' ' + selDong : ''} 데이터 ${data.deleted}건이 삭제되었습니다.`);
        pendingDeleteMonth = null;
        fetchList();
    } catch (err) {
        console.error('삭제 실패', err);
        alert('삭제 중 오류가 발생했습니다.');
    } finally {
        if (btn) btn.disabled = false;
    }
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
    renderTable();
}