/** @namespace FullCalendar */
/* global FullCalendar */

const CSRF_TOKEN = document.querySelector('meta[name="_csrf"]')?.content;
const CSRF_HEADER = document.querySelector('meta[name="_csrf_header"]')?.content;



let calendar = null;
let currentFacilityId = null; // 현재 선택된 시설 ID

document.addEventListener('DOMContentLoaded', init);

async function init() {
    // 1. 시설 목록을 먼저 불러옵니다.
    await loadFacilities();

    // 2. 캘린더를 초기화합니다.
    initCalendar();
}

// 시설 버튼 목록 로드 및 생성
async function loadFacilities() {
    try {
        const res = await fetch('/hometop/api/facility');
        const facilities = await res.json();
        renderFacilityButtons(facilities);
    } catch (err) {
        console.error("시설 로드 실패:", err);
    }
}

function renderFacilityButtons(facilities) {
    const wrap = document.getElementById('facilityButtons');
    wrap.innerHTML = '';

    facilities.forEach(f => {
        const btn = document.createElement('button');
        btn.innerText = f.name;
        btn.classList.add('facility-btn');

        btn.onclick = () => {
            // 버튼 클릭 시 처리
            document.querySelectorAll('.facility-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');

            onFacilitySelect(f.id);
        };
        wrap.appendChild(btn);
    });
}

function initCalendar() {
    const calendarEl = document.getElementById('calendar');

    calendar = new FullCalendar.Calendar(calendarEl, {
        initialView: 'dayGridMonth',
        displayEventTime: false,
        locale: 'ko',
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

            const currentView = calendar ? calendar.view : info.view;
            const viewDate = currentView.currentStart;

            const year = viewDate.getFullYear();
            const month = viewDate.getMonth() + 1;

            const url = `/hometop/api/reservations/calendar?facilityId=${currentFacilityId}&year=${year}&month=${month}`;

            fetch(url)
                .then(res => {
                    if (!res.ok) throw new Error('Network response was not ok');
                    return res.json();
                })
                .then(data => {
                    const events = Array.isArray(data) ? data.map(r => {
                        const currentStatus = r.badge || 'PENDING';
                        const eventColor = getStatusColor(currentStatus);

                        // 1. 시간 포맷팅 (18:00~19:00 형식)
                        const startTime = r.startAt ? r.startAt.substring(11, 16) : '';
                        const endTime = r.endAt ? r.endAt.substring(11, 16) : '';

                        // 2. 타이틀에서 이름만 추출 (만약 '시설명 (이름)' 형식이라면)
                        // 만약 서버에서 이름 필드를 따로 준다면 r.userName 등을 쓰면 더 정확합니다.
                        let displayName = r.title || '예약자';
                        const nameMatch = displayName.match(/\((.*?)\)/); // 괄호 안의 이름 추출
                        if (nameMatch) {
                            displayName = nameMatch[1];
                        }

                        // 3. 최종 포맷: [18:00~19:00] 홍길동
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
                            extendedProps: { status: currentStatus },
                            color: eventColor
                        };
                    }) : [];
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

function getStatusColor(status) {
    if (status === undefined || status === null) return '#95a5a6';

    // 문자열로 변환 후 공백 제거 및 대문자 변환
    const s = String(status).trim().toUpperCase();

    switch(s) {
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
            return '#E5E4E2'; // 기본 회색
    }
}

async function onFacilitySelect(id) {
    currentFacilityId = id;

    // 예약 신청 버튼의 링크에 facilityId를 파라미터로
    const applyBtn = document.querySelector('a[href*="/reservation/apply"]');
    if (applyBtn) {
        applyBtn.href = `/hometop/reservation/apply?facilityId=${id}`;
    }

    if (calendar) {
        calendar.refetchEvents();
    }
}