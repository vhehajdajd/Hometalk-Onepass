// 기본 뼈대
let currentFacilityId = null; // null = 전체
let currentYear;
let currentMonth;
let reservationData = [];

let facilities = [];

const WEEKDAYS = ['일','월','화','수','목','금','토'];

document.addEventListener('DOMContentLoaded', init);

async function init() {
    const today = new Date();
    currentYear = today.getFullYear();
    currentMonth = today.getMonth() + 1;

    const saved = localStorage.getItem('facilityId');
    currentFacilityId = saved ? Number(saved) : null;

    initSelects();
    bindEvents();

    await loadFacilities();

    updateHeader(today);

    if (currentFacilityId !== null) {
        showCalendar();
        await loadReservations();
    } else {
        hideCalendar();
    }
}

function initSelects() {
    const yearSelect = document.getElementById('yearSelect');
    const monthSelect = document.getElementById('monthSelect');

    const today = new Date();
    const baseYear = today.getFullYear();

    for (let y = baseYear - 5; y <= baseYear + 5; y++) {
        const opt = document.createElement('option');
        opt.value = y;
        opt.text = `${y}년`;
        if (y === currentYear) opt.selected = true;
        yearSelect.appendChild(opt);
    }

    monthSelect.value = currentMonth;
}

function loadFacilities() {
    return fetch('/hometop/api/facility')
        .then(res => res.json())
        .then(data => {
            facilities = data;
            renderFacilityButtons();
        });
}

function renderFacilityButtons() {
    const wrap = document.getElementById('facilityButtons');
    wrap.innerHTML = '';

    // 시설 없음 예외 처리
    if (!facilities || facilities.length === 0) {
        const empty = document.createElement('div');
        empty.className = 'facility-empty';
        empty.innerText = '등록된 시설이 없습니다';
        wrap.appendChild(empty);
        return;
    }

    // 전체 버튼
    const allBtn = document.createElement('button');
    allBtn.innerText = '전체';
    allBtn.classList.add('facility-btn');

    if (currentFacilityId === null) {
        allBtn.classList.add('active');
    }

    allBtn.onclick = () => {
        currentFacilityId = null;
        localStorage.removeItem('facilityId');

        hideCalendar();   // 여기서 숨김
        reservationData = [];
        renderTodayCount();
    };

    wrap.appendChild(allBtn);

    // 시설 버튼
    facilities.forEach(f => {
        const btn = document.createElement('button');
        btn.innerText = f.name;
        btn.classList.add('facility-btn');

        if (currentFacilityId === f.id) {
            btn.classList.add('active');
        }

        btn.onclick = async () => {
            currentFacilityId = f.id;
            localStorage.setItem('facilityId', f.id);

            showCalendar();
            await loadReservations();
            renderFacilityButtons();
        };

        wrap.appendChild(btn);
    });
}

function updateHeader(today) {
    document.getElementById('navYear').innerText = currentYear;
    document.getElementById('navMonthNum').innerText = currentMonth + '월';

    document.getElementById('todayDateNum').innerText = today.getDate();
    document.getElementById('todayWeekday').innerText =
        WEEKDAYS[today.getDay()];

    document.getElementById('yearSelect').value = currentYear;
    document.getElementById('monthSelect').value = currentMonth;
}

function onFacilitySelect(id) {
    currentFacilityId = id;
    localStorage.setItem('facilityId', id);

    showCalendar();   // 여기서만 켜짐
    loadReservations();
}

function showCalendar() {
    document.getElementById('calendarWrap').style.display = 'block';
}

function hideCalendar() {
    document.getElementById('calendarWrap').style.display = 'none';
}

// API 호출
async function loadReservations() {
    if (currentFacilityId === null) return;

    let url = `/hometop/api/reservations/calendar?year=${currentYear}&month=${currentMonth}&facilityId=${currentFacilityId}`;

    const res = await fetch(url);
    reservationData = await res.json();

    renderCalendar();
    renderTodayCount();
}

// 캘린더 렌더
function renderCalendar() {
    const body = document.getElementById('calendarBody');
    body.innerHTML = '';

    const firstDay = new Date(currentYear, currentMonth - 1, 1).getDay();
    const lastDate = new Date(currentYear, currentMonth, 0).getDate();

    let cells = [];

    for (let i = 0; i < firstDay; i++) {
        cells.push(null);
    }

    for (let d = 1; d <= lastDate; d++) {
        cells.push(d);
    }

    cells.forEach((day, idx) => {
        const div = document.createElement('div');
        div.className = 'cal-cell';

        if (!day) {
            div.classList.add('empty');
        } else {
            div.innerHTML = `<div class="cal-date-num">${day}</div>`;

            const dayReservations = reservationData.filter(r => {
                const d = new Date(r.startAt);
                return d.getDate() === day &&
                    d.getMonth() + 1 === currentMonth &&
                    d.getFullYear() === currentYear;
            });

            const slot = document.createElement('div');
            slot.className = 'cal-slot-container';

            dayReservations.forEach(r => {
                const badge = createBadge(r);
                slot.appendChild(badge);
            });

            div.appendChild(slot);
        }

        body.appendChild(div);
    });
}

// 배지 생성
function createBadge(r) {

    const statusMap = {
        CONFIRMED: 'facility',
        PENDING: 'normal',
        CANCELED: 'safety',
        COMPLETED: 'default'
    };

    const badge = document.createElement('div');

    badge.className = `cal-badge badge-${statusMap[r.status] || 'default'}`;

    badge.innerHTML = `
        ${r.title}<br>
        ${formatTime(r.startAt, r.endAt)}
    `;

    return badge;
}

// 시간 포맷
function formatTime(startAt, endAt) {
    const s = new Date(startAt);
    const e = new Date(endAt);

    return `${pad(s.getHours())}시 ~ ${pad(e.getHours())}시`;
}

function pad(n) {
    return n < 10 ? '0' + n : n;
}

// 이벤트 (월 이동)
function bindEvents() {
    document.getElementById('prevMonth').onclick = () => {
        currentMonth--;
        if (currentMonth < 1) {
            currentMonth = 12;
            currentYear--;
        }
        loadReservations();
        updateHeader(new Date(currentYear, currentMonth - 1, 1));
    };

    document.getElementById('nextMonth').onclick = () => {
        currentMonth++;
        if (currentMonth > 12) {
            currentMonth = 1;
            currentYear++;
        }
        loadReservations();
        updateHeader(new Date(currentYear, currentMonth - 1, 1));
    };
}

// 오늘 카운트
function renderTodayCount() {
    const today = new Date();

    const count = reservationData.filter(r => {
        const d = new Date(r.startAt);

        return (
            d.getFullYear() === currentYear &&
            d.getMonth() + 1 === currentMonth &&
            d.getDate() === today.getDate()
        );
    }).length;

    document.getElementById('todayCount').innerText =
        `오늘 예약 · ${count}건`;
}