/* ================================================================
   billing_upload.js  —  Tab 1: 고지서 업로드
================================================================ */
'use strict';

const ITEM_COLS = [
    '일반관리비(과)', '수선유지비(과)', '청소비(과)', '승강기유지비(과)',
    '소방시설유지비(과)', '전기안전점검비(과)', '건물유지관리비(과)',
    '기본난방비(과)', '세대급탕비(과)', '세대전기료(과)', '공동전기료(과)',
    '일반관리비', '화재보험료', '생활폐기물수수료', '장기수선충당금',
    '세대수도료', '공동수도료'
];

const MONTHS = ['1월','2월','3월','4월','5월','6월','7월','8월',
                '9월','10월','11월','12월'];

let validRows    = [];
let billingMonth = null;
let dbHasData    = false;   // 해당 월 DB 데이터 존재 여부
let mode         = 'preview'; // 'preview' | 'result'
let selDong      = null;
let sortKey      = null;
let sortDir      = 1;
let pendingFile  = null;
let openDongPanel = false;

/* ================================================================
   초기화
================================================================ */
document.addEventListener('DOMContentLoaded', () => {
    initUploadZone();
});

/* ================================================================
   업로드 존
================================================================ */
function initUploadZone() {
    const zone      = document.getElementById('uploadZone');
    const fileInput = document.getElementById('fileInput');
    if (!zone || !fileInput) return;

    zone.addEventListener('click', () => fileInput.click());
    zone.addEventListener('dragover',  e => { e.preventDefault(); zone.classList.add('drag-over'); });
    zone.addEventListener('dragleave', () => zone.classList.remove('drag-over'));
    zone.addEventListener('drop', e => {
        e.preventDefault();
        zone.classList.remove('drag-over');
        if (e.dataTransfer.files[0]) handleFile(e.dataTransfer.files[0]);
    });
    fileInput.addEventListener('change', e => {
        if (e.target.files[0]) handleFile(e.target.files[0]);
        e.target.value = '';
    });
}

/* ================================================================
   파일 처리
================================================================ */
async function handleFile(file) {
    const nameMatch = file.name.match(/(\d{4})[_-](\d{2})/);
    billingMonth    = nameMatch ? `${nameMatch[1]}-${nameMatch[2]}` : null;

    if (!billingMonth) {
        alert('파일명에서 연도_월을 찾을 수 없습니다.\n예: 2026_01.xlsx');
        return;
    }

    try {
        const res  = await fetch(`${CONTEXT_PATH}/api/billing/admin/upload/check?billingMonth=${billingMonth}`);
        const data = await res.json();
        dbHasData  = data.exists;

        if (dbHasData) {
            pendingFile = file;
            document.getElementById('dupMsg').textContent =
                `${billingMonth} 관리비 고지서가 이미 업로드되어 있습니다.`;
            document.getElementById('dupOverlay').style.display = 'flex';
            return;
        }
    } catch (err) {
        console.warn('중복 확인 실패', err);
        dbHasData = false;
    }

    processFile(file);
}

function cancelDup() {
    document.getElementById('dupOverlay').style.display = 'none';
    pendingFile  = null;
    billingMonth = null;
    dbHasData    = false;
}

function confirmDup() {
    document.getElementById('dupOverlay').style.display = 'none';
    if (pendingFile) {
        processFile(pendingFile);
        pendingFile = null;
    }
}

/* ================================================================
   SheetJS 파싱
================================================================ */
function processFile(file) {
    const reader = new FileReader();
    reader.onload = async e => {
        const wb      = XLSX.read(e.target.result, { type: 'binary' });
        const allRows = [];
        wb.SheetNames.forEach(sheetName => {
            const ws   = wb.Sheets[sheetName];
            const rows = XLSX.utils.sheet_to_json(ws, { raw: true, defval: '' });
            const dong = sheetName.replace(/동$/, '') + '동';
            rows.forEach(row => allRows.push({ ...row, _dong: dong }));
        });
        await runValidation(allRows);
    };
    reader.readAsBinaryString(file);
}

/* ================================================================
   유효성 검사
================================================================ */
async function runValidation(rows) {
    const trimmedRows = rows.map(row => {
        const t = {};
        Object.keys(row).forEach(k => { t[k.trim()] = row[k]; });
        return t;
    });

    validRows = trimmedRows.map((row, idx) => {
        const dongHo = String(row['동/호'] ?? '').trim();
        const totalRaw   = parseFloat(String(row['당월부과액']).replace(/,/g, ''));
        const total      = isNaN(totalRaw) ? 0 : totalRaw;
        const totalInvalid = isNaN(totalRaw) || totalRaw < 0;
        const dong   = row._dong || '';
        const hoPart = dongHo.split('-')[1] || '';
        const unit   = dongHo ? `${dong} ${hoPart}호` : dongHo;
        const month  = billingMonth || '';

        // ★ 항목 파싱 + 유효성 검사
        const details = [];
        let hasInvalidItem = false;
        let hasMissingItem = false;

        for (const col of ITEM_COLS) {
            const raw = row[col];

            if (raw === '' || raw === null || raw === undefined) {
                hasMissingItem = true;
                break;
            }

            const num = parseFloat(String(raw).replace(/,/g, ''));

            if (isNaN(num) || num < 0) {
                hasInvalidItem = true;
                break;
            }

            if (num === 0) {
                hasMissingItem = true;
                break;
            }

            details.push({ item_name: col, item_amount: num });
        }

        let valid = '정상';
        if (!dongHo || !month)         valid = '오류';
        else if (hasInvalidItem)       valid = '항목 오류';
        else if (totalInvalid)         valid = '금액 오류';
        else if (total === 0)          valid = '금액 누락';
        else if (hasMissingItem)       valid = '금액 누락';
        else if (details.length === 0) valid = '항목 누락';

        return {
            num:           idx + 1,
            household_id:  dongHo,
            dong,
            unit,
            billing_month: month,
            total_amount:  total,
            valid,
            upsertType:    null,
            details,
        };
    });

    // DB 데이터 있을 때만 UPSERT 비교
    if (dbHasData) await fetchAndSetUpsertTypes();

    selDong  = null;
    sortKey  = null;
    sortDir  = 1;
    mode     = 'preview';
    buildDongGrid();
    showPreviewSection();
}

/* ================================================================
   UPSERT 타입 결정 (DB 있을 때만 호출)
   - DB에 없는 세대   → INSERT
   - DB에 있고 값 다름 → UPDATE
   - DB에 있고 값 같음 → null (서버 미전송, 표시 없음)
================================================================ */
async function fetchAndSetUpsertTypes() {
    const dbMap = new Map();

    try {
        const listRes = await fetch(
            `${CONTEXT_PATH}/api/billing/admin/list?month=${billingMonth}&size=500`
        );
        if (!listRes.ok) return;

        const dbItems = (await listRes.json()).content || [];

        await Promise.all(dbItems.map(async item => {
            if (!item.billingId || !item.unit) return;
            try {
                const detRes = await fetch(`${CONTEXT_PATH}/api/billing/${item.billingId}/detail`);
                if (!detRes.ok) return;
                const det = await detRes.json();
                dbMap.set(item.unit, {
                    totalAmount: Number(item.totalAmount) || 0,
                    items: (det.items || []).map(i => ({
                        itemName:   i.itemName,
                        itemAmount: Number(i.itemAmount) || 0
                    }))
                });
            } catch (e) { /* 개별 실패 무시 */ }
        }));
    } catch (err) {
        console.warn('UPSERT 비교 실패, 비교 없이 진행', err);
    }

    validRows = validRows.map(r => {
        if (r.valid !== '정상') return { ...r, upsertType: null };

        const dbRow = dbMap.get(r.unit);
        if (!dbRow) return { ...r, upsertType: 'INSERT' };

        const same = isSame(
            { totalAmount: r.total_amount,
              items: r.details.map(d => ({ itemName: d.item_name, itemAmount: d.item_amount })) },
            dbRow
        );
        return { ...r, upsertType: same ? null : 'UPDATE' };
    });
}

function isSame(excel, db) {
    if (Number(excel.totalAmount) !== Number(db.totalAmount)) return false;
    if (excel.items.length !== db.items.length) return false;
    const map = new Map(db.items.map(i => [i.itemName, Number(i.itemAmount)]));
    for (const it of excel.items) {
        if (map.get(it.itemName) !== Number(it.itemAmount)) return false;
    }
    return true;
}

/* ================================================================
   미리보기 섹션 표시
================================================================ */
function showPreviewSection() {
    document.getElementById('uploadDoneBanner').style.display = 'none';
    document.getElementById('uploadZone').style.display       = '';
    document.getElementById('filterBar').style.display        = 'flex';
    document.getElementById('tableSection').style.display     = 'block';
    document.getElementById('tableActions').style.display     = 'flex';
    document.getElementById('tableTitle').textContent         =
        '유효성 검사 + 고지서 미리보기 + 업로드 확정';

    // UPSERT 컬럼: DB 있을 때만 표시
    document.getElementById('thUpsert').style.display = dbHasData ? '' : 'none';

    renderTable();
}

/* ================================================================
   결과 섹션 표시 (업로드 확정 후)
================================================================ */
function showResultSection(insertCount, updateCount) {
    mode = 'result';

    // 완료 배너 표시
    document.getElementById('uploadDoneBanner').style.display = 'flex';
    document.getElementById('uploadDoneMsg').textContent      =
        `${billingMonth} 업로드 완료 — 신규 ${insertCount}건 · 변경 ${updateCount}건`;

    // 업로드 존 숨김
    document.getElementById('uploadZone').style.display   = 'none';

    // 확정 버튼 숨김
    document.getElementById('tableActions').style.display = 'none';

    // UPSERT 컬럼 항상 표시 (결과 확인)
    document.getElementById('thUpsert').style.display = '';
    document.getElementById('tableTitle').textContent  = '업로드 결과 (읽기 전용)';

    renderTable();
}

/* ================================================================
   테이블 렌더링
================================================================ */
function renderTable() {
    let rows = validRows.filter(r => !selDong || r.dong === selDong);

    if (sortKey) {
        rows = [...rows].sort((a, b) => {
            const av = a[sortKey] ?? '', bv = b[sortKey] ?? '';
            if (av < bv) return -sortDir;
            if (av > bv) return  sortDir;
            return 0;
        });
    }

    rows = rows.map((r, i) => ({ ...r, num: i + 1 }));

    const totalCount  = validRows.length;
    const errorCount  = validRows.filter(r => r.valid !== '정상').length;
    const normalCount = totalCount - errorCount;
    const insertCount = validRows.filter(r => r.upsertType === 'INSERT').length;
    const updateCount = validRows.filter(r => r.upsertType === 'UPDATE').length;
    const noChangeCount = validRows.filter(r => r.valid === '정상' && !r.upsertType).length;

    const showUpsert = dbHasData || mode === 'result';

    if (mode === 'preview') {
        document.getElementById('tableMeta').innerHTML =
            `총 ${totalCount}세대 · <span class="meta-error">오류 ${errorCount}건</span>`;
        document.getElementById('tableSummary').innerHTML = dbHasData
            ? `변경 ${updateCount} · 신규 ${insertCount} · 변경없음 ${noChangeCount} · 오류 ${errorCount}`
            : `정상 ${normalCount} · 오류 ${errorCount}`;

        // ★ 필터된 행 기준으로 버튼 활성화 판단
        const filteredRows  = selDong ? validRows.filter(r => r.dong === selDong) : validRows;
        const filteredError = filteredRows.filter(r => r.valid !== '정상').length;
        const filteredNormal = filteredRows.filter(r => r.valid === '정상').length;
        const filteredInsert = filteredRows.filter(r => r.upsertType === 'INSERT').length;
        const filteredUpdate = filteredRows.filter(r => r.upsertType === 'UPDATE').length;
        const savable  = dbHasData ? filteredInsert + filteredUpdate : filteredNormal;
        const disabled = filteredError > 0 || savable === 0;

        const btn = document.getElementById('btnConfirm');
        btn.disabled = disabled;
        btn.classList.toggle('disabled', disabled);
    } else {
        document.getElementById('tableMeta').innerHTML     = `업로드 결과 · 총 ${totalCount}세대`;
        document.getElementById('tableSummary').innerHTML  =
            `신규 ${insertCount} · 변경 ${updateCount} · 변경없음 ${noChangeCount}`;
    }

    document.getElementById('tableBody').innerHTML = rows.map(r => {
        const isError = r.valid !== '정상';

        const validCell = isError
            ? `<span class="badge badge-error">${r.valid}</span>`
            : `<span class="badge badge-ok">정상</span>`;

        const previewCell = isError
            ? `<button class="btn-preview disabled-preview" disabled>미리보기 불가</button>`
            : `<button class="btn-preview" onclick="openPreview('${r.household_id}','${r.billing_month}')">미리보기 →</button>`;

        let upsertTd = '';
        if (showUpsert) {
            let cell = '—';
            if (r.upsertType === 'INSERT') cell = `<span class="badge badge-insert">신규 INSERT</span>`;
            if (r.upsertType === 'UPDATE') cell = `<span class="badge badge-update">기존 UPDATE</span>`;
            upsertTd = `<td>${cell}</td>`;
        }

        return `
        <tr class="${isError ? 'row-error' : ''}">
            <td>${r.num}</td>
            <td>${r.unit}</td>
            <td>${r.billing_month}</td>
            <td>${isError ? '—' : Number(r.total_amount).toLocaleString() + '원'}</td>
            <td>${validCell}</td>
            <td>${previewCell}</td>
            ${upsertTd}
        </tr>`;
    }).join('');
}

/* ================================================================
    동 필터
================================================================ */

function buildDongGrid() {
    const dongs = [...new Set(validRows.map(r => r.dong))].sort();
    const sel = document.getElementById('selDong');
    sel.innerHTML = '<option value="">전체 동</option>'
        + dongs.map(d => `<option value="${d}">${d}</option>`).join('');
    sel.value = selDong || '';
}

function onDongChange() {
    selDong = document.getElementById('selDong').value || null;
    renderTable();
}

function sortBy(key) {
    if (sortKey === key) sortDir *= -1;
    else { sortKey = key; sortDir = 1; }
    renderTable();
}

/* ================================================================
   미리보기 모달
================================================================ */
function openPreview(hid, month) {
    const row = validRows.find(r => r.household_id === hid && r.billing_month === month);
    if (!row || row.valid !== '정상') return;

    const [y, m] = month.split('-').map(Number);
    const last   = new Date(y, m, 0).getDate();

    document.getElementById('modalHeaderTitle').textContent =
        `고지서 미리보기 — ${row.unit} (${month})`;
    document.getElementById('modalPeriod').textContent =
        `부과월: ${month} · 납부기한: ${month.replace('-','.')}.${String(last).padStart(2,'0')}`;
    document.getElementById('modalRows').innerHTML = row.details.length
        ? row.details.map(d =>
            `<div class="bill-row">
                <span>${d.item_name}</span>
                <span>${Number(d.item_amount).toLocaleString()}원</span>
            </div>`).join('')
        : '<div style="font-size:13px;color:#aaa;text-align:center;padding:16px 0;">항목 정보 없음</div>';
    document.getElementById('modalTotal').textContent =
        Number(row.total_amount).toLocaleString() + '원';
    document.getElementById('previewOverlay').style.display = 'flex';
}

function closePreview(e) {
    if (e && e.target !== document.getElementById('previewOverlay')) return;
    closePreviewBtn();
}
function closePreviewBtn() {
    document.getElementById('previewOverlay').style.display = 'none';
}

/* ================================================================
   업로드 확정 팝업
================================================================ */
function confirmUpload() {
    // ★ selDong 필터 적용
    const targetRows = selDong
        ? validRows.filter(r => r.dong === selDong)
        : validRows;

    const errorCount  = targetRows.filter(r => r.valid !== '정상').length;
    const normalCount = targetRows.filter(r => r.valid === '정상').length;
    const insertCount = targetRows.filter(r => r.upsertType === 'INSERT').length;
    const updateCount = targetRows.filter(r => r.upsertType === 'UPDATE').length;
    const savable     = dbHasData ? insertCount + updateCount : normalCount;

    if (errorCount > 0) {
        alert(`${selDong ? selDong + '에 ' : ''}오류가 있는 세대가 있습니다. 확인 후 다시 시도해 주세요.`);
        return;
    }
    if (savable === 0) {
        alert('변경된 내용이 없습니다.\n(모든 세대가 DB와 동일)');
        return;
    }

    const dongLabel = selDong ? ` (${selDong})` : '';
    document.getElementById('confirmTitle').textContent =
        updateCount > 0 ? `업로드 확정${dongLabel} (변경 포함)` : `업로드 확정${dongLabel}`;
    document.getElementById('confirmMsg').textContent =
        `${billingMonth}${dongLabel} 관리비 고지서를 저장하시겠습니까?`;
    document.getElementById('confirmMsgWarn').textContent = dbHasData
        ? `신규 ${insertCount}건 · 변경 ${updateCount}건`
        : `총 ${savable}세대 신규 등록`;

    document.getElementById('confirmOverlay').style.display = 'flex';
}

function cancelConfirm() {
    document.getElementById('confirmOverlay').style.display = 'none';
}

/* ================================================================
   업로드 확정 API
================================================================ */
async function doConfirmUpload() {
    const confirmBtn = document.querySelector('#confirmOverlay .btn-point');
    if (confirmBtn) confirmBtn.disabled = true;

    // ★ selDong 필터 적용
    const targetRows = selDong
        ? validRows.filter(r => r.dong === selDong)
        : validRows;

    const uploadRows = targetRows
        .filter(r => {
            if (r.valid !== '정상') return false;
            if (dbHasData) return r.upsertType === 'INSERT' || r.upsertType === 'UPDATE';
            return true;
        })
        .map(r => ({
            householdId:  r.household_id,
            billingMonth: r.billing_month,
            dueDate:      lastDay(r.billing_month),
            totalAmount:  r.total_amount,
            items: r.details.map(d => ({ itemName: d.item_name, itemAmount: d.item_amount }))
        }));

    try {
        const res = await fetch(
            `${CONTEXT_PATH}/api/billing/admin/upload/confirm?adminId=1`,
            {
                method:  'POST',
                headers: { 'Content-Type': 'application/json', [CSRF_HEADER]: CSRF_TOKEN },
                body:    JSON.stringify(uploadRows),
            }
        );
        if (!res.ok) throw new Error('업로드 실패');
        const data = await res.json();

        document.getElementById('confirmOverlay').style.display = 'none';
        showResultSection(data.insertCount, data.updateCount);

    } catch (err) {
        console.error('업로드 확정 실패', err);
        alert('업로드 중 오류가 발생했습니다.');
    } finally {
        if (confirmBtn) confirmBtn.disabled = false;
    }
}

/* ================================================================
   초기화
================================================================ */
function resetUpload() {
    validRows    = [];
    billingMonth = null;
    dbHasData    = false;
    mode         = 'preview';
    selDong      = null;
    sortKey      = null;
    sortDir      = 1;

    document.getElementById('uploadZone').style.display       = '';
    document.getElementById('uploadDoneBanner').style.display = 'none';
    document.getElementById('filterBar').style.display        = 'none';
    document.getElementById('tableSection').style.display     = 'block';  // ← none → block
    document.getElementById('tableBody').innerHTML            =
        `<tr><td colspan="7" style="text-align:center;padding:36px;color:#aaa;">파일을 업로드하면 여기에 표시됩니다.</td></tr>`;
    document.getElementById('fileInput').value                = '';
}

/* ================================================================
   유틸
================================================================ */
function lastDay(bm) {
    const [y, m] = bm.split('-').map(Number);
    // 다음 달 10일 (2026-03 관리비 → 2026-04-10 납부기한)
    const nextMonth = m === 12 ? 1 : m + 1;
    const nextYear  = m === 12 ? y + 1 : y;
    return `${nextYear}-${String(nextMonth).padStart(2,'0')}-10`;
}