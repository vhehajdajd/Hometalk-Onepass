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

// 6. 예약 제출
async function submitReservation() {
    const data = {
        facilityName: document.getElementById('hidden-facility').value,
        reservationDate: document.getElementById('hidden-date').value,
        startTime: document.querySelector('input[name="startTime"]:checked')?.value
    };
    if (!data.startTime) { alert("시간을 선택해주세요!"); return; }

    const response = await fetch('/hometop/api/reservations', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    });
    if (response.ok) {
        alert("예약 신청이 완료되었습니다.");
        location.href = "/hometop/reservation/my";
    }
}