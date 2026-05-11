/** @namespace FullCalendar */
/* global FullCalendar */

const CSRF_TOKEN = document.querySelector('meta[name="_csrf"]')?.content;
const CSRF_HEADER = document.querySelector('meta[name="_csrf_header"]')?.content;

let calendar = null;
let currentFacilityId = null;

document.addEventListener('DOMContentLoaded', init);

async function init() {
    const facilities = await loadFacilities();

    initCalendar();

    if (facilities && facilities.length > 0) {
        const firstFacility = facilities[0];

        currentFacilityId = firstFacility.id;

        const firstBtn = document.querySelector('.facility-btn');
        if (firstBtn) {
            firstBtn.classList.add('active');
        }

        calendar.refetchEvents();
    }
}

// 시설 목록 로드
async function loadFacilities() {
    try {
        const res = await fetch('/hometop/api/facility');

        if (!res.ok) {
            throw new Error('시설 목록 조회 실패');
        }

        const facilities = await res.json();

        renderFacilityButtons(facilities);

        return facilities;
    } catch (err) {
        console.error("시설 로드 실패:", err);
        return [];
    }
}

// 시설 버튼 생성
function renderFacilityButtons(facilities) {
    const wrap = document.getElementById('facilityButtons');

    if (!wrap) return;

    wrap.innerHTML = '';

    facilities.forEach(f => {
        const btn = document.createElement('button');

        btn.innerText = f.name;
        btn.classList.add('facility-btn');

        btn.onclick = () => {
            document.querySelectorAll('.facility-btn')
                .forEach(b => b.classList.remove('active'));

            btn.classList.add('active');

            onFacilitySelect(f.id);
        };

        wrap.appendChild(btn);
    });
}

// 캘린더 초기화
function initCalendar() {
    const calendarEl = document.getElementById('calendar');

    if (!calendarEl) return;

    calendar = new FullCalendar.Calendar(calendarEl, {
        initialView: 'dayGridMonth',
        displayEventTime: false,
        locale: 'ko',

        dayCellContent: function(info) {
            return info.date.getDate();
        },

        headerToolbar: {
            left: 'prev,next today',
            center: 'title',
            right: 'dayGridMonth,timeGridWeek'
        },

        height: '700px',

        events: function(info, successCallback, failureCallback) {
            if (!currentFacilityId) {
                successCallback([]);
                return;
            }

            const viewDate = new Date(info.start);
            viewDate.setDate(viewDate.getDate() + 10);

            const year = viewDate.getFullYear();
            const month = viewDate.getMonth() + 1;

            const url =
            `/hometop/api/reservations/calendar?facilityId=${currentFacilityId}&year=${year}&month=${month}`;

            fetch(url)
                .then(res => {
                    if (!res.ok) {
                        throw new Error('예약 데이터 조회 실패');
                    }
                return res.json();
                })
                .then(data => {
                    const filteredData = Array.isArray(data)
                        ? data.filter(r => {
                        const s = String(r.badge || r.status || '').trim().toUpperCase();
                        return s !== 'CANCEL' && s !== 'CANCELED';
                    })
                        : [];

                    const events = filteredData.map(r => {
                        const currentStatus = r.badge || r.status || 'PENDING';
                        const eventColor = getStatusColor(currentStatus);

                        const startTime = r.startAt ? r.startAt.substring(11, 16) : '';
                        const endTime = r.endAt ? r.endAt.substring(11, 16) : '';

                        let displayName = r.title || '예약';

                        const nameMatch = displayName.match(/\((.*?)\)/);
                        if (nameMatch) {
                            displayName = nameMatch[1];
                        }

                        const finalTitle = `[${startTime}~${endTime}] ${displayName}`;

                        return {
                            id: r.id,
                            title: finalTitle,
                            start: r.startAt,
                            end: r.endAt,
                            backgroundColor: eventColor,
                            borderColor: eventColor,
                            textColor: '#444444',
                            display: 'block',
                            extendedProps: {
                                status: currentStatus
                            }
                        };
                    });
                    successCallback(events);
                })
                .catch(err => {
                console.error("데이터 로드 실패:", err);
                failureCallback(err);
                });
        }
    });

    calendar.render();
}

// 상태별 색상
function getStatusColor(status) {
    if (status === undefined || status === null) {
        return '#95a5a6';
    }

    const s = String(status).trim().toUpperCase();

    switch (s) {
        case 'CONFIRMED':
        case '1':
            return '#D0E2FF';

        case 'PENDING':
        case 'WAITING':
        case '0':
            return '#FFE5B4';

        case 'CANCELED':
        case 'CANCEL':
            return '#F0F0F0';

        default:
            console.warn("정의되지 않은 상태값입니다:", s);
            return '#E5E4E2';
    }
}

// 시설 선택 시 이벤트 다시 조회
async function onFacilitySelect(id) {
    currentFacilityId = id;

    const applyBtn = document.querySelector('a[href*="/reservation/apply"]');

    if (applyBtn) {
        applyBtn.href = `/hometop/reservation/apply?facilityId=${id}`;
    }

    if (calendar) {
        calendar.refetchEvents();
    }
}