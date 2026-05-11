const CSRF_TOKEN = document.querySelector('meta[name="_csrf"]')?.content;
const CSRF_HEADER = document.querySelector('meta[name="_csrf_header"]')?.content;

function getCsrfHeaders() {
    const headers = { 'Content-Type': 'application/json' };
    if (CSRF_HEADER && CSRF_TOKEN) {
        headers[CSRF_HEADER] = CSRF_TOKEN;
    }
    return headers;
}

function handleFacilityClick(element) {
    // data- 속성 값
    const name = element.getAttribute('data-name');
    const id = element.getAttribute('data-id');
    const usageTime = 1; // 시설별로 다를 경우 data-usage-time 속성 활용

    document.getElementById('hidden-facility-id').value = id;
    document.getElementById('hidden-facility').value = name;

    selectFacility(name, usageTime, id);
}

// 1. 시설 선택 (이용 시간 저장)
function selectFacility(name, usageTime, id) {
    document.getElementById('hidden-facility').value = name;
    document.getElementById('hidden-facility-id').value = id;
    document.getElementById('selected-facility-name').innerText = name;
    document.getElementById('selected-facility-usage-time').value = usageTime;
    nextStep(2);
}

// 2. 날짜 선택
function selectDate(val) {
    if (!val) return;
    const date = new Date(val);
    const today = new Date();
    // 날짜 포맷팅
    const week = ['일', '월', '화', '수', '목', '금', '토'];
    const formattedDate = `${String(date.getMonth() + 1).padStart(2, '0')}월 ${String(date.getDate()).padStart(2, '0')}일 (${week[date.getDay()]})`;
    document.getElementById('hidden-date').value = val;
    document.getElementById('display-date-text').innerText = formattedDate;
    // 오늘인 경우 지난 시간 숨기기
    filterAvailableTimes(val);
    nextStep(3);
}
function filterAvailableTimes(selectedDateStr) {
    const today = new Date();
    const todayStr = today.toISOString().split('T')[0];
    const currentHour = today.getHours();
    const timeChips = document.querySelectorAll('.time-chip-wrapper');

    timeChips.forEach(chip => {
        const timeElement = chip.querySelector('.time-chip');
        if (!timeElement) return;

        const timeText = timeElement.innerText; // "09:00"
        const chipHour = parseInt(timeText.split(':')[0]);

        if (selectedDateStr === todayStr) {
            // 오늘 날짜인 경우 현재 시간 이전은 선택 불가 처리
            if (chipHour <= currentHour) {
                chip.style.display = 'none';
            } else {
                chip.style.display = 'block';
            }
        } else {
            // 오늘이 아니면 모든 시간 표시
            chip.style.display = 'block';
        }
    });
}

// 2-1. 오늘 날짜 기준으로 일주일
document.addEventListener('DOMContentLoaded', function() {
    const dateInput = document.getElementById('resDate');
    if (dateInput) {
        const now = new Date();

        // 1. 최소 날짜 (오늘)
        const minDate = now.toISOString().split('T')[0];

        // 2. 최대 날짜 (오늘 + 7일)
        const maxDateObj = new Date();
        maxDateObj.setDate(now.getDate() + 7);
        const maxDate = maxDateObj.toISOString().split('T')[0];

        // 3. 속성 적용
        dateInput.min = minDate;
        dateInput.max = maxDate;
    }
});

// 3. 시간 표시 (이용 단위 시간 반영)
function updateTimeDisplay(val) {
    document.getElementById('display-time').innerText = val;
    const usageTime = parseInt(document.getElementById('selected-facility-usage-time').value) || 1;
    let hour = parseInt(val.split(':')[0]);
    let endHour = hour + usageTime;
    document.getElementById('end-time').innerText = (endHour).toString().padStart(2, '0') + ":00";

    document.querySelectorAll('.time-chip').forEach(chip => chip.classList.remove('active'));
    if (event && event.target && event.target.nextElementSibling) {
        event.target.nextElementSibling.classList.add('active');
    }
}

// 4. 단계 이동
function nextStep(step) {
    document.querySelectorAll('.step-section').forEach(s => s.classList.remove('active'));
    document.getElementById('step' + step).classList.add('active');
    window.scrollTo(0, 0);
}
function prevStep(step) {
    document.querySelectorAll('.step-section').forEach(s => s.classList.remove('active'));
    document.getElementById('step' + step).classList.add('active');
}

// 5. 예약 취소 [공통]
async function confirmCancel(id, isAdmin = false) {

    let reason = null;

    // 관리자 강제취소 시 사유 입력
    if (isAdmin) {
        reason = prompt("취소 사유를 입력해주세요.");

        if (!reason || !reason.trim()) {
            alert("취소 사유를 입력해야 합니다.");
            return;
        }
    } else {
        const ok = confirm("예약을 취소하시겠습니까?");
        if (!ok) return;
    }

    try {

        const response = await fetch(`/hometop/api/reservations/${id}/cancel`, {
            method: 'PATCH',
            headers: getCsrfHeaders(),
            body: JSON.stringify({
                reason: reason
            })
        });

        if (response.ok) {

            alert(isAdmin ? "예약이 강제 취소되었습니다." : "예약이 취소되었습니다.");

            location.reload();

        } else {

            const errorMsg = await response.text();

            alert("실패: " + (errorMsg || response.status));
        }

    } catch (error) {

        console.error(error);

        alert("서버 통신 중 오류가 발생했습니다.");
    }
}

// [관리자]
function confirmAdminCancel(id) {
    confirmCancel(id, true);
}

// 6. 예약 제출
async function submitReservation() {
    const startTimeInput = document.querySelector('input[name="startTime"]:checked');
    if (!startTimeInput) { alert("시간을 선택해주세요!"); return; }

    const resDate = document.getElementById('hidden-date').value; // "2026-05-08"
    const startTimeVal = startTimeInput.value; // "18:00"
    const usageTime = parseInt(document.getElementById('selected-facility-usage-time').value) || 1;
    const facilityId = document.getElementById('hidden-facility-id').value;

    // 1. LocalDateTime 형식으로 합치기 (yyyy-MM-ddTHH:mm:ss)
    const startDateTime = `${resDate}T${startTimeVal}:00`;

    // 종료 시간 계산
    let endHour = parseInt(startTimeVal.split(':')[0]) + usageTime;
    const endDateTime = `${resDate}T${String(endHour).padStart(2, '0')}:00:00`;

    const data = {
        // DTO의 필드명과 일치
        facilityId: parseInt(facilityId),
        startTime: startDateTime,
        endTime: endDateTime
    };

    try {
        const response = await fetch('/hometop/api/reservations', {
            method: 'POST',
            headers: getCsrfHeaders(),
            body: JSON.stringify(data)
        });

        if (response.ok) {
            alert("예약 신청이 완료되었습니다.");
            location.href = "/hometop/reservation/my";
        } else {
            alert("예약에 실패했습니다. (Error: " + response.status + ")");
        }
    } catch (error) {
        console.error("Error:", error);
        alert("서버 통신 중 오류가 발생했습니다.");
    }
}

// 예약 승인
function approveReservation(id) {
    if (!confirm("이 예약을 승인하시겠습니까?")) return;

    fetch(`/hometop/api/reservations/${id}/approve`, {
        method: 'POST',
        headers: getCsrfHeaders()
    })
        .then(res => {
            if (res.ok) {
                alert("예약이 승인되었습니다.");
                location.reload();
            } else {
                alert("승인 처리 중 오류가 발생했습니다.");
            }
        });
}