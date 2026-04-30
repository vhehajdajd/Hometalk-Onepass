// 1. 시설 선택 (이용 시간 저장)
function selectFacility(name, usageTime) {
    document.getElementById('hidden-facility').value = name;
    document.getElementById('selected-facility-name').innerText = name;
    document.getElementById('selected-facility-usage-time').value = usageTime;
    nextStep(2);
}

// 2. 날짜 선택
function selectDate(val) {
    if (!val) return;

    const selectedDate = new Date(val);
    selectedDate.setHours(0, 0, 0, 0);

    const today = new Date();
    today.setHours(0, 0, 0, 0);

       if (selectedDate < today) {
        alert("과거 날짜는 선택하실 수 없습니다. 오늘 또는 이후 날짜를 선택해주세요.");
        document.getElementById('resDate').value = ""; // input 초기화
        return;
    }

    const week = ['일', '월', '화', '수', '목', '금', '토'];
    const formattedDate = `${String(selectedDate.getMonth() + 1).padStart(2, '0')}월 ${String(selectedDate.getDate()).padStart(2, '0')}일 (${week[selectedDate.getDay()]})`;

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

// 6. 예약 제출
async function submitReservation() {
    const facilityName = document.getElementById('hidden-facility').value;
    const reservationDate = document.getElementById('hidden-date').value;
    const startTimeInput = document.querySelector('input[name="startTime"]:checked');
    // 관리자가 등록한 이용 시간 가져오기 (기본값 1)
    const usageTime = parseInt(document.getElementById('selected-facility-usage-time').value) || 1;

    if (!facilityName || !reservationDate || !startTimeInput) {
        alert("시설, 날짜, 시간을 모두 선택해 주세요.");
        return;
    }

    const startTime = startTimeInput.value; // 예: "11:00"

    // 종료 시간 계산 (시작 시간 + 이용 시간)
    let startHour = parseInt(startTime.split(':')[0]);
    let endHour = startHour + usageTime;
    const endTime = endHour.toString().padStart(2, '0') + ":00";

    // 보안 토큰
    const token = document.querySelector('meta[name="_csrf"]')?.content;
    const header = document.querySelector('meta[name="_csrf_header"]')?.content;


    const data = {
        facilityName: facilityName,
        reservationDate: reservationDate,
        startTime: startTime,
        endTime: endTime
    };

    try {
        const response = await fetch('/hometop/api/reservations', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [header]: token
            },
            body: JSON.stringify(data) // JSON 형식이 맞는지 확인
        });

        if (response.ok) {
            alert("예약 신청이 완료되었습니다!");
            location.href = "/hometop/reservation/my";
        } else {
            // 서버에서 보낸 에러 메시지 확인
            const errorResult = await response.json();
            alert("예약 실패: " + (errorResult.message || "데이터 형식을 확인하세요."));
        }
    } catch (error) {
        console.error("에러:", error);
        alert("서버 연결 실패");
    }
}