const CSRF_TOKEN = document.querySelector('meta[name="_csrf"]')?.content;
const CSRF_HEADER = document.querySelector('meta[name="_csrf_header"]')?.content;

function getCsrfHeaders() {
    const headers = { 'Content-Type': 'application/json' };
    if (CSRF_HEADER && CSRF_TOKEN) {
        headers[CSRF_HEADER] = CSRF_TOKEN;
    }
    return headers;
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
/* function updateTimeDisplay(val) {
    document.getElementById('display-time').innerText = val;
    const usageTime = parseInt(document.getElementById('selected-facility-usage-time').value) || 1;
    let hour = parseInt(val.split(':')[0]);
    let endHour = hour + usageTime;
    document.getElementById('end-time').innerText = (endHour).toString().padStart(2, '0') + ":00";

    document.querySelectorAll('.time-chip').forEach(chip => chip.classList.remove('active'));
    if (event && event.target && event.target.nextElementSibling) {
        event.target.nextElementSibling.classList.add('active');
    }
} */

let selectedHours = [];

function handleFacilityClick(element) {
    const name = element.getAttribute('data-name');
    const id = element.getAttribute('data-id');
    // 서버에서 가져온 최대 예약 시간을 가져옴 (없으면 기본 1시간)
    const maxTime = parseInt(element.getAttribute('data-max-time')) || 1;

    document.getElementById('hidden-facility-id').value = id;
    document.getElementById('hidden-facility').value = name;
    document.getElementById('selected-facility-usage-time').value = maxTime;

    selectFacility(name, maxTime, id);
}

function handleTimeClick(element) {
    const hour = parseInt(element.getAttribute('data-hour'));
    const maxAllowed = parseInt(document.getElementById('selected-facility-usage-time').value);
    const guideElement = document.querySelector('.guide-text');

    // 1. 이미 선택된 시간이 하나도 없거나, 선택을 새로 시작할 때
    if (selectedHours.length === 0 || selectedHours.length >= 2) {
        selectedHours = [hour];
        if(guideElement) guideElement.innerHTML = `<strong>종료 시간</strong>을 선택해 주세요. (최대 ${maxAllowed}시간)`;
    }
    // 2. 이미 하나가 선택된 상태에서 두 번째 클릭 (범위 지정)
    else {
        const firstHour = selectedHours[0];
        const start = Math.min(firstHour, hour);
        const end = Math.max(firstHour, hour);
        const diff = end - start + 1;

        if (diff > maxAllowed) {
            showNotice(`이 시설은 최대 ${maxAllowed}시간까지만 선택 가능합니다.`);
            return;
        }

        // 사이의 모든 시간을 배열에 추가 (연속 선택)
        selectedHours = [];
        for (let i = start; i <= end; i++) {
            selectedHours.push(i);
        }
        if(guideElement) guideElement.innerHTML = `원하시는 시간대의 <strong>시작 시간</strong>과 <strong>종료 시간</strong>을 클릭해 주세요.`;
    }

    renderTimeSelection();
}

function renderTimeSelection() {
    const chips = document.querySelectorAll('.time-chip-wrapper');

    if (selectedHours.length === 0) {
        document.getElementById('display-time').innerText = "--:00";
        document.getElementById('end-time').innerText = "--:00";
        chips.forEach(chip => chip.querySelector('.time-chip').classList.remove('active'));
        return;
    }

    const startHour = Math.min(...selectedHours);
    const endHour = Math.max(...selectedHours) + 1; // 종료는 +1시간

    chips.forEach(chip => {
        const hour = parseInt(chip.getAttribute('data-hour'));
        const chipDiv = chip.querySelector('.time-chip');

        if (selectedHours.includes(hour)) {
            chipDiv.classList.add('active');
        } else {
            chipDiv.classList.remove('active');
        }
    });

    if (selectedHours.length > 0) {
        document.getElementById('display-time').innerText = `${String(startHour).padStart(2, '0')}:00`;
        document.getElementById('end-time').innerText = `${String(endHour).padStart(2, '0')}:00`;

        console.log("현재 선택된 시간들:", selectedHours);
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
            showNotice("취소 사유를 입력해야 합니다.");
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

            showNotice(isAdmin ? "예약이 강제 취소되었습니다." : "예약이 취소되었습니다.");

            location.reload();

        } else {

            const errorMsg = await response.text();

            showNotice("실패: " + (errorMsg || response.status));
        }

    } catch (error) {

        console.error(error);

        showNotice("서버 통신 중 오류가 발생했습니다.");
    }
}

// [관리자]
function confirmAdminCancel(id) {
    confirmCancel(id, true);
}

// 6. 예약 제출
async function submitReservation() {
    if (selectedHours.length === 0) {
        showNotice("시간을 선택해주세요!");
        return;
    }

    const resDate = document.getElementById('hidden-date').value;
    const facilityId = document.getElementById('hidden-facility-id').value;

    // 2. 시간 계산: 선택된 배열에서 최소/최대 추출
    const startHour = Math.min(...selectedHours);
    const endHour = Math.max(...selectedHours) + 1;

    // 3. 서버로 보낼 데이터
    const data = {
        facilityId: parseInt(facilityId),
        startTime: `${resDate}T${String(startHour).padStart(2, '0')}:00:00`,
        endTime: `${resDate}T${String(endHour).padStart(2, '0')}:00:00`
    };

    try {
        const response = await fetch('/hometop/api/reservations', {
            method: 'POST',
            headers: getCsrfHeaders(),
            body: JSON.stringify(data)
        });

        if (response.ok) {
            showNotice("예약 신청이 완료되었습니다.");
            location.href = "/hometop/reservation/my";
        } else {
            showNotice("예약에 실패했습니다. (Error: " + response.status + ")");
        }
    } catch (error) {
        console.error("Error:", error);
        showNotice("서버 통신 중 오류가 발생했습니다.");
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
                showNotice("예약이 승인되었습니다.");
                location.reload();
            } else {
                showNotice("승인 처리 중 오류가 발생했습니다.");
            }
        });
}

// [사용자] 이용 종료
async function finishUsage(id) {
    if (!confirm("지금 이용을 종료하시겠습니까?\n종료 시 남은 시간은 다른 분들이 예약할 수 있도록 개방됩니다.")) {
        return;
    }

    try {
        const response = await fetch(`/hometop/api/reservations/${id}/finish`, {
            method: 'PATCH',
            headers: getCsrfHeaders()
        });

        if (response.ok) {
            showNotice("이용 종료 처리가 완료되었습니다.");
            setTimeout(() => {
                location.reload();
            }, 1000);
        } else {
            const errorText = await response.text();
            showNotice(errorText || "이용 종료 처리 중 오류가 발생했습니다.", "error");
        }
    } catch (error) {
        console.error("Error:", error);
        showNotice("서버와 통신 중 오류가 발생했습니다.", "error");
    }
}

// 알림 메시지
function showNotice(message, type = 'success') {
    const existingBox = document.querySelector('.alert-box');
    if (existingBox) existingBox.remove();

    const alertBox = document.createElement('div');
    alertBox.className = `alert-box ${type === 'success' ? 'alert-success' : 'alert-danger'}`;

    alertBox.innerHTML = `
        <div class="alert-content">
            <i class="${type === 'success' ? 'fas fa-check-circle' : 'fas fa-exclamation-circle'}"></i>
            <span>${message}</span>
        </div>
        <button type="button" class="alert-close" onclick="this.parentElement.remove()">&times;</button>
    `;

    // 메인 컨테이너 가장 상단에 삽입
    const container = document.querySelector('.container');
    container.prepend(alertBox);

    // 3초 후 자동으로 사라지게
    setTimeout(() => {
        if (alertBox) alertBox.style.opacity = '0';
        setTimeout(() => alertBox.remove(), 500);
    }, 3000);
}