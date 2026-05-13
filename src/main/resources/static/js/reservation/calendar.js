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
        if (!res.ok) throw new Error('시설 목록 조회 실패');
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
                    if (!res.ok) throw new Error('예약 데이터 조회 실패');
                    return res.json();
                })
                .then(data => {
                    const filteredData = Array.isArray(data)
                        ? data.filter(r => {
                        const s = String(r.badge || r.status || '').trim().toUpperCase();
                        return s !== 'CANCEL' && s !== 'CANCELED';
                    }) : [];

                    const groupedByTime = filteredData.reduce((acc, r) => {
                        const startTime = r.startAt ? r.startAt.substring(11, 16) : '';
                        const endTime = r.endAt ? r.endAt.substring(11, 16) : '';

                        // 날짜와 시간을 키로 생성 (예: 2026-05-13_17:00~18:00)
                        const datePart = r.startAt ? r.startAt.substring(0, 10) : '';
                        const key = `${datePart}_${startTime}~${endTime}`;

                        // 이름 추출 로직
                        let displayName = r.title || '예약';
                        const nameMatch = displayName.match(/\((.*?)\)/);
                        if (nameMatch) displayName = nameMatch[1];

                        if (!acc[key]) {
                            acc[key] = {
                                ...r,
                                startTime,
                                endTime,
                                names: [displayName]
                            };
                        } else {
                            acc[key].names.push(displayName);
                        }
                        return acc;
                    }, {});

                    // 3. 그룹화된 데이터를 FullCalendar 형식으로 변환
                    const events = Object.values(groupedByTime).map(group => {
                        const currentStatus = group.badge || group.status || 'PENDING';
                        const eventColor = getStatusColor(currentStatus);

                        // 텍스트 색상
                        let textColor = '#444444';
                        if (currentStatus === 'FINISHED') textColor = '#2D4F49';
                        if (currentStatus === 'COMPLETED') textColor = '#2B4562';
                        if (currentStatus === 'PENDING' || currentStatus === 'WAITING' || currentStatus === '0') textColor = '#5F5817';
                        if (currentStatus === 'CONFIRMED' || currentStatus === '1') textColor = '#37474F';

                        // 합쳐진 이름들 생성 (콤마로 연결)
                        const combinedNames = group.names.join(', ');
                        const finalTitle = `[${group.startTime}~${group.endTime}] ${combinedNames}`;

                        return {
                            id: group.id,
                            title: finalTitle,
                            start: group.startAt,
                            end: group.endAt,
                            backgroundColor: eventColor,
                            borderColor: eventColor,
                            textColor: textColor,
                            display: 'block',
                            extendedProps: {
                                status: currentStatus,
                                originalNames: group.names
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
            return '#FFF9C4';

        case 'CANCELED':
        case 'CANCEL':
            return '#EEEEEE';

        case 'COMPLETED':
            return '#D1E9FF';

        case 'FINISHED':
            return '#C1F0E8';

        default:
            console.warn("정의되지 않은 상태값입니다:", s);
            return '#F5F5F5';
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