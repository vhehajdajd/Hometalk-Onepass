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

let currentFacilityMaxCapacity = 0;

// 2. 날짜 선택
async function selectDate(val) {
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
    const facilityId = document.getElementById('hidden-facility-id').value;
    await updateCapacityInfo(facilityId, val);
    await disableUserBookedTimes(val);
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

async function disableUserBookedTimes(date) {
    try {
        const response = await fetch(`/hometop/api/reservations/user-booked-times?date=${date}`);
        if (!response.ok) return;
        const bookedHours = await response.json();
        const timeChips = document.querySelectorAll('.time-chip-wrapper');
        timeChips.forEach(chip => {
            const hour = parseInt(chip.getAttribute('data-hour'));

            if (bookedHours.includes(hour)) {
                chip.classList.add('is-full');

                const capElement = document.getElementById(`cap-${hour}`);
                if (capElement) {
                    capElement.innerText = "나의 예약 있음";
                    capElement.style.color = "#ff9800";
                }
            }
        });
    } catch (error) {
        console.error("유저 예약 정보 조회 실패:", error);
    }
}

document.addEventListener('DOMContentLoaded', function () {

    const now = new Date();

    const maxDateObj = new Date();
    maxDateObj.setDate(now.getDate() + 7);

    flatpickr("#resDate", {
        locale: "ko",
        dateFormat: "Y-m-d",

        minDate: "today",
        maxDate: maxDateObj,

        onChange: function(selectedDates, dateStr) {
            selectDate(dateStr);
        }
    });

});

async function updateCapacityInfo(facilityId, date) {
    try {
        const response = await fetch(`/hometop/api/reservations/capacity?facilityId=${facilityId}&date=${date}`);
        const bookedData = await response.json();

        const timeChips = document.querySelectorAll('.time-chip-wrapper');

        timeChips.forEach(chip => {
            const hour = chip.getAttribute('data-hour');
            const bookedCount = bookedData[hour] || 0;
            const remaining = currentFacilityMaxCapacity - bookedCount;

            const capElement = document.getElementById(`cap-${hour}`);
            if (capElement) {
                if (remaining <= 0) {
                    capElement.innerText = "예약 마감";
                    capElement.style.color = "red";
                    chip.classList.add('is-full'); // CSS에서 클릭 방지용 클래스
                } else {
                    capElement.innerText = `${bookedCount}/${currentFacilityMaxCapacity}`;
                    capElement.style.color = "var(--color-sub)";
                    chip.classList.remove('is-full');
                }
            }
        });
    } catch (error) {
        console.error("인원 정보 조회 실패:", error);
    }
}

let selectedHours = [];

async function handleFacilityClick(element) {
    const name = element.getAttribute('data-name');
    const id = element.getAttribute('data-id');
    try {
        const response = await fetch(`/hometop/api/reservations/check-active/${id}`);
        const data = await response.json();

        if (data.hasActive) {
            showNotice(data.message);
            return;
        }
    } catch (error) {
        console.error("중복 예약 체크 중 오류 발생:", error);
    }
    // 서버에서 가져온 최대 예약 시간 (없으면 기본 1시간)
    const maxTime = parseInt(element.getAttribute('data-max-time')) || 1;
    currentFacilityMaxCapacity = parseInt(element.getAttribute('data-max-capacity')) || 1;

    document.getElementById('hidden-facility-id').value = id;
    document.getElementById('hidden-facility').value = name;
    document.getElementById('selected-facility-usage-time').value = maxTime;

    const guideElement = document.querySelector('.guide-text');
    if (guideElement) {
        if (maxTime === 1) {
            guideElement.innerHTML = `원하시는 <strong>예약 시간</strong>을 클릭해 주세요.`;
        } else {
            guideElement.innerHTML = `원하시는 시간대의 <strong>시작 시간</strong>과 <strong>종료 시간</strong>을 각각 클릭해 주세요.`;
        }
    }

    document.querySelectorAll('.facility-item').forEach(item => item.classList.remove('selected'));
    element.classList.add('selected');

    selectFacility(name, maxTime, id);
}

function handleTimeClick(element) {
    if (element.classList.contains('is-full')) {
        showNotice("이미 정원이 초과된 시간대입니다.", "error");
        return;
    }
    const hour = parseInt(element.getAttribute('data-hour'));
    const maxAllowed = parseInt(document.getElementById('selected-facility-usage-time').value);
    const guideElement = document.querySelector('.guide-text');

    if (maxAllowed === 1) {
        selectedHours = selectedHours.includes(hour) ? [] : [hour];
        if (guideElement) {
            guideElement.innerHTML = `원하시는 <strong>예약 시간</strong>을 클릭해 주세요.`;
        }
    }
    else {
        if (selectedHours.includes(hour)) {
            selectedHours = [];
            if(guideElement) guideElement.innerHTML = `원하시는 시간대의 <strong>시작 시간</strong>과 <strong>종료 시간</strong>을 클릭해 주세요.`;
            renderTimeSelection();
            return;
        }

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
                const chip = document.querySelector(`.time-chip-wrapper[data-hour="${i}"]`);
                if (chip && chip.classList.contains('is-full')) {
                    showNotice("선택하신 범위에 이미 예약이 꽉 찬 시간이 포함되어 있습니다.", "error");
                    selectedHours = []; // 선택 초기화
                    renderTimeSelection();
                    return;
                }
                selectedHours.push(i);
            }
            if(guideElement) guideElement.innerHTML = `원하시는 시간대의 <strong>시작 시간</strong>과 <strong>종료 시간</strong>을 클릭해 주세요.`;
        }
    }
    renderTimeSelection();
}

function renderTimeSelection() {
    const chips = document.querySelectorAll('.time-chip-wrapper');

    if (selectedHours.length === 0) {
        document.getElementById('display-time').innerText = "00:00";
        document.getElementById('end-time').innerText = "00:00";
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
    if(step === 2) {
        document.getElementById('resDate').value = '';
        document.getElementById('display-time').innerText = "00:00";
        document.getElementById('end-time').innerText = "00:00";
        selectedHours = [];
    }
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
async function finishUsage(event, id) {
    const btn = event.currentTarget || event.target;

    if (!confirm("지금 이용을 종료하시겠습니까?\n종료 시 남은 시간은 다른 분들이 예약할 수 있도록 개방됩니다.")) {
        return;
    }

    try {
        btn.disabled = true;
        const originalText = btn.innerText;
        btn.innerText = "처리 중...";
        const response = await fetch(`/hometop/api/reservations/${id}/finish`, {
            method: 'PATCH',
            headers: {
                ...getCsrfHeaders(),
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            showNotice("이용 종료 처리가 완료되었습니다.");
            setTimeout(() => {
                location.reload();
            }, 1000);
        } else {
            const errorText = await response.text();
            showNotice(errorText || "이용 종료 처리 중 오류가 발생했습니다.", "error");
            btn.disabled = false;
            btn.innerText = originalText;
        }
    } catch (error) {
        console.error("Error:", error);
        showNotice("서버와 통신 중 오류가 발생했습니다.", "error");
        btn.disabled = false;
        if(typeof originalText !== 'undefined') btn.innerText = originalText;
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