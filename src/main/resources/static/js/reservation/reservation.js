const CSRF_TOKEN = document.querySelector('meta[name="_csrf"]')?.content;
const CSRF_HEADER = document.querySelector('meta[name="_csrf_header"]')?.content;

function handleFacilityClick(element) {
    // data- 속성 값
    const name = element.getAttribute('data-name');
    const id = element.getAttribute('data-id');
    const usageTime = 1; // 고정값이라면 직접 입력

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
    const week = ['일', '월', '화', '수', '목', '금', '토'];
    const formattedDate = `${String(date.getMonth() + 1).padStart(2, '0')}월 ${String(date.getDate()).padStart(2, '0')}일 (${week[date.getDay()]})`;
    document.getElementById('hidden-date').value = val;
    document.getElementById('display-date-text').innerText = formattedDate;
    nextStep(3);
}

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

// 5. [관리자] 강제 취소 컨펌
function confirmAdminCancel(id) {
    if (confirm("해당 예약을 강제로 취소하시겠습니까?")) {
        fetch(`/hometop/api/reservations/${id}/cancel`, { method: 'POST' })
            .then(res => {
            if (res.ok) {
                alert("취소되었습니다.");
                location.reload();
            }
        });
    }
}

// 5-1. 사용자 예약 취소
function confirmCancel(id) {
    if (confirm("예약을 취소하시겠습니까?")) {
        // 관리자와 같은 API를 쓰거나 별도의 사용자 취소 API를 호출
        fetch(`/hometop/api/reservations/${id}/cancel`, {
            method: 'POST',
            headers: {
                // CSRF 토큰이 필요하면 추가
                'Content-Type': 'application/json'
            }
        })
        .then(res => {
            if (res.ok) {
                alert("예약이 취소되었습니다.");
                location.reload(); // 화면 새로고침
            } else {
                alert("취소에 실패했습니다.");
            }
        });
    }
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
        // DTO의 필드명과 일치시켜야 함
        facilityId: parseInt(facilityId),
        userId: 1,  // 테스트용 유저 ID
        startTime: startDateTime,
        endTime: endDateTime
    };

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    try {
        const response = await fetch('/hometop/api/reservations', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                ...(csrfHeader && csrfToken ? { [csrfHeader]: csrfToken } : {})
            },
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
function approveReservation(reservationId) {
    if (!confirm("이 예약을 승인하시겠습니까?")) return;

    fetch(`/hometop/api/admin/reservations/${reservationId}/approve`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            ...(CSRF_HEADER && CSRF_TOKEN ? { [CSRF_HEADER]: CSRF_TOKEN } : {})
        }
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