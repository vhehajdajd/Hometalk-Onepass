// /* dashboard-admin.js */
//
// // ── Mock 데이터 ──
// var MOCK_NOTICES = [
//     {id:1, title:"화재 예방 및 안전수칙 준수 안내", badge:"SAFETY", createdAt:"2026-04-24T10:00:00"},
//     {id:2, title:"분리수거장 이용 규정 변경 안내", badge:"FACILITY", createdAt:"2026-04-23T09:30:00"},
//     {id:3, title:"단지 내 플리마켓 개최 안내", badge:"NORMAL", createdAt:"2026-04-22T14:00:00"},
//     {id:4, title:"세대 인터폰 교체 안내", badge:"FACILITY", createdAt:"2026-04-21T10:00:00"},
//     {id:5, title:"택배 보관함 이용 안내", badge:"NORMAL", createdAt:"2026-04-20T09:00:00"}
// ];
// var MOCK_COMMUNITY = [
//     {id:105, title:"단지 내 분실물(에어팟) 보관 중입니다.", categoryName:"분실물", boardCode:"market"},
//     {id:104, title:"오늘 저녁에 단지 앞 공원에서 산책하실 분!", categoryName:"자유", boardCode:"square"},
//     {id:103, title:"안 쓰는 깨끗한 인형 나눔해요.", categoryName:"나눔", boardCode:"market"}
// ];
// var MOCK_TODAY_SCHEDULES = [
//     {id:101, title:"플리마켓 개최", startAt:"2026-04-30T10:00:00", endAt:"2026-04-30T15:00:00", badge:"NORMAL"},
//     {id:102, title:"관리비 납부 마감", startAt:"2026-04-30T00:00:00", endAt:null, badge:"FACILITY"}
// ];
// var MOCK_BILLING = {billingMonth:"2월", unpaidHouseholds:12, totalUnpaidAmount:3450000};
// var MOCK_PARKED_COUNT = 23;
// var MOCK_PENDING_COUNT = 5;
// var MOCK_RESERVATIONS = [
//     {id:305, title:"테니스장 A코트", userName:"김지현 (103동)", status:"확정"},
//     {id:304, title:"게스트하우스 101호", userName:"이철수 (101동)", status:"승인대기"},
//     {id:303, title:"독서실 15번석", userName:"박민준 (105동)", status:"이용중"}
// ];
// var MOCK_INQUIRIES = [
//     {id:25, title:"이사 당일 사다리차 이용 시간 문의", userName:"이철수 (101동)", status:"미답변"},
//     {id:22, title:"입주민 카드 발급 방법 알려주세요.", userName:"박민준 (105동)", status:"답변완료"},
//     {id:19, title:"커뮤니티 센터 헬스장 등록 비용", userName:"김지현 (103동)", status:"답변완료"}
// ];
// var MOCK_COMPLAINTS = [
//     {id:10, title:"층간소음 관련하여 중재 요청드립니다.", userName:"이철수 (101동)", isSecret:true, status:"확인중"},
//     {id:9, title:"놀이터 기구 파손 제보", userName:"박민준 (105동)", isSecret:false, status:"접수완료"},
//     {id:8, title:"복도 전등이 깜빡거려요.", userName:"김지현 (103동)", isSecret:false, status:"답변완료"}
// ];
// var MOCK_SCHEDULES = [
//     {id:101, title:"플리마켓 개최", startAt:"2026-04-29T10:00:00", badge:"NORMAL"},
//     {id:102, title:"관리비 납부 마감", startAt:"2026-04-29T00:00:00", badge:"FACILITY"},
//     {id:103, title:"소방훈련 실시", startAt:"2026-04-27T14:00:00", badge:"SAFETY"},
//     {id:104, title:"입주민 회의", startAt:"2026-04-15T19:00:00", badge:"NORMAL"},
//     {id:105, title:"엘리베이터 점검", startAt:"2026-04-10T09:00:00", badge:"FACILITY"}
// ];
//
// // ── 달력 상태 ──
// var today = new Date();
// var calYear  = today.getFullYear();
// var calMonth = today.getMonth() + 1;
// var scheduleData = MOCK_SCHEDULES;
// var BADGE_LABEL = { 'FACILITY': '시설', 'SAFETY': '안전', 'NORMAL': '일반' };
//
// // ── 유틸 함수 ──
// function formatDate(str) { return str ? str.substring(0, 10) : ''; }
// function formatTime(str) { return str ? str.substring(11, 16) : ''; }
// function formatDateTime(str) { if (!str) return '-'; return str.substring(0,10) + ' ' + str.substring(11,16); }
// function comma(n) { return n != null ? n.toLocaleString() : '-'; }
// function badgeLabel(b) { return b==='FACILITY'?'시설':b==='SAFETY'?'안전':b==='NORMAL'?'일반':(b||''); }
//
// function getStatusClass(status) {
//     if (!status) return 's-received';
//     if (['확정','승인완료','답변완료'].includes(status)) return 's-answered';
//     if (['승인대기','접수완료'].includes(status))        return 's-pending';
//     if (status === '확인중')  return 's-processing';
//     if (status === '미답변')  return 's-unanswered';
//     if (status === '이용중')  return 's-confirmed';
//     return 's-received';
// }
//
// // ── 일정 모달 ──
// function openScheduleModal(id) {
//     var s = MOCK_TODAY_SCHEDULES.find(function(s){ return s.id === id; });
//     if (!s) return;
//     document.getElementById('modalTitle').textContent = s.title;
//     document.getElementById('modalBadge').textContent = s.badge ? (BADGE_LABEL[s.badge] || s.badge) : '-';
//     document.getElementById('modalStart').textContent = formatDateTime(s.startAt);
//     var endRow = document.getElementById('modalEndRow');
//     if (s.endAt) { document.getElementById('modalEnd').textContent = formatDateTime(s.endAt); endRow.style.display = ''; }
//     else { endRow.style.display = 'none'; }
//     document.getElementById('modalInfoRow').style.display = 'none';
//     document.getElementById('modalLocationRow').style.display = 'none';
//     document.getElementById('modalNoticeRow').style.display = 'none';
//     document.getElementById('scheduleDetailModal').style.display = 'flex';
// }
// function closeScheduleModal() { document.getElementById('scheduleDetailModal').style.display = 'none'; }
//
// // ── 렌더링 함수 ──
// function renderNotice(data) {
//     var el = document.getElementById('noticeList');
//     if (!data||!data.length){el.innerHTML='<div class="loading">공지가 없습니다.</div>';return;}
//     el.innerHTML = data.map(function(n){
//         return '<a class="notice-item" href="/hometop/notice/'+n.id+'">'+
//             '<span class="notice-badge '+n.badge+'">'+badgeLabel(n.badge)+'</span>'+
//             '<span class="notice-title">'+n.title+'</span>'+
//             '<span class="notice-date">'+formatDate(n.createdAt)+'</span></a>';
//     }).join('');
// }
//
// function renderCommunity(data) {
//     var el = document.getElementById('communityList');
//     if (!data||!data.length){el.innerHTML='<div class="loading">게시글이 없습니다.</div>';return;}
//     el.innerHTML = data.map(function(c){
//         return '<a class="community-item" href="/hometop/community/'+c.boardCode+'/'+c.id+'">'+
//             '<span class="community-badge">'+c.categoryName+'</span>'+
//             '<span class="community-title">'+c.title+'</span></a>';
//     }).join('');
// }
//
// function renderTodaySchedule(data) {
//     var el = document.getElementById('todayScheduleList');
//     if (!data||!data.length){el.innerHTML='<div class="loading">오늘 일정이 없습니다.</div>';return;}
//     el.innerHTML = data.map(function(s){
//         var badge = s.badge || 'DEFAULT';
//         var timeStr = formatTime(s.startAt);
//         if (s.endAt) timeStr += ' ~ ' + formatTime(s.endAt);
//         return '<div class="today-schedule-item" style="cursor:pointer;" onclick="openScheduleModal('+s.id+')">'+
//             '<span class="today-schedule-badge '+badge+'">'+badgeLabel(s.badge)+'</span>'+
//             '<span class="today-schedule-title">'+s.title+'</span>'+
//             '<span class="today-schedule-time">'+timeStr+'</span></div>';
//     }).join('');
// }
//
// function renderAdminBilling(data) {
//     var el = document.getElementById('billingContent');
//     if (!data){el.innerHTML='<div class="loading">정보 없음</div>';return;}
//     el.innerHTML =
//         '<div class="billing-card-row">'+
//         '<span class="billing-card-row-label">'+data.billingMonth+' 미납</span>'+
//         '<span class="billing-card-row-value">'+data.unpaidHouseholds+'세대</span>'+
//         '</div>'+
//         '<div class="billing-card-row">'+
//         '<span class="billing-card-row-label">미납 총액</span>'+
//         '<span class="billing-card-row-value" style="color:var(--color-btn-point);">'+comma(data.totalUnpaidAmount)+'원</span>'+
//         '</div>';
// }
//
// function renderParking(parkedCount, pendingCount) {
//     document.getElementById('parkedCount').textContent  = parkedCount;
//     document.getElementById('pendingCount').textContent = pendingCount;
// }
//
// function renderReservation(data) {
//     var el = document.getElementById('reservationList');
//     if (!data||!data.length){el.innerHTML='<div class="loading">예약 내역 없음</div>';return;}
//     el.innerHTML = data.slice(0,3).map(function(r){
//         return '<div class="small-item">'+
//             '<span class="small-item-title">'+r.title+'</span>'+
//             '<span style="font-size:11px;color:var(--color-text-muted);white-space:nowrap;">'+r.userName+'</span>'+
//             '<span class="small-status '+getStatusClass(r.status)+'">'+r.status+'</span></div>';
//     }).join('');
// }
//
// function renderInquiry(data) {
//     var el = document.getElementById('inquiryList');
//     if (!data||!data.length){el.innerHTML='<div class="loading">문의 내역 없음</div>';return;}
//     el.innerHTML = data.slice(0,3).map(function(i){
//         return '<div class="small-item">'+
//             '<span class="small-item-title">'+i.title+'</span>'+
//             '<span style="font-size:11px;color:var(--color-text-muted);white-space:nowrap;">'+i.userName+'</span>'+
//             '<span class="small-status '+getStatusClass(i.status)+'">'+i.status+'</span></div>';
//     }).join('');
// }
//
// function renderComplaint(data) {
//     var el = document.getElementById('complaintList');
//     if (!data||!data.length){el.innerHTML='<div class="loading">민원 내역 없음</div>';return;}
//     el.innerHTML = data.slice(0,3).map(function(c){
//         var title = c.isSecret ? '🔒 비밀글' : c.title;
//         return '<div class="small-item">'+
//             '<span class="small-item-title">'+title+'</span>'+
//             '<span style="font-size:11px;color:var(--color-text-muted);white-space:nowrap;">'+c.userName+'</span>'+
//             '<span class="small-status '+getStatusClass(c.status)+'">'+c.status+'</span></div>';
//     }).join('');
// }
//
// function renderCalendar() {
//     document.getElementById('calTitle').textContent = calYear+'년 '+calMonth+'월';
//     var grid = document.getElementById('calGrid');
//     grid.innerHTML = '';
//     ['SUN','MON','TUE','WED','THU','FRI','SAT'].forEach(function(d,i){
//         var div=document.createElement('div');
//         div.className='cal-day-header'+(i===0?' sun':i===6?' sat':'');
//         div.textContent=d; grid.appendChild(div);
//     });
//     var firstDay=new Date(calYear,calMonth-1,1).getDay();
//     var lastDate=new Date(calYear,calMonth,0).getDate();
//     var prevLast=new Date(calYear,calMonth-1,0).getDate();
//     var cells=[];
//     for(var i=0;i<firstDay;i++) cells.push({day:prevLast-firstDay+1+i,other:true});
//     for(var d=1;d<=lastDate;d++) cells.push({day:d,other:false});
//     var remain=cells.length%7===0?0:7-(cells.length%7);
//     for(var k=1;k<=remain;k++) cells.push({day:k,other:true});
//     cells.forEach(function(cell){
//         var div=document.createElement('div');
//         div.className='cal-cell'+(cell.other?' other-month':'');
//         var isToday=!cell.other&&cell.day===today.getDate()&&calMonth===today.getMonth()+1&&calYear===today.getFullYear();
//         if(isToday) div.classList.add('today');
//         var dateDiv=document.createElement('div');
//         dateDiv.className='cal-date'; dateDiv.textContent=cell.day; div.appendChild(dateDiv);
//         if(!cell.other){
//             var daySch=scheduleData.filter(function(s){
//                 var sd=new Date(s.startAt);
//                 return sd.getFullYear()===calYear&&sd.getMonth()+1===calMonth&&sd.getDate()===cell.day;
//             });
//             if(daySch.length>0){
//                 var dotsDiv=document.createElement('div'); dotsDiv.className='cal-dots';
//                 daySch.slice(0,3).forEach(function(s){
//                     var dot=document.createElement('div');
//                     dot.className='cal-dot '+(s.badge||'DEFAULT'); dotsDiv.appendChild(dot);
//                 }); div.appendChild(dotsDiv);
//             }
//         }
//         grid.appendChild(div);
//     });
// }
//
// // ── 달력 버튼 이벤트 ──
// document.getElementById('calPrev').addEventListener('click',function(){
//     calMonth--; if(calMonth<1){calMonth=12;calYear--;} renderCalendar();
// });
// document.getElementById('calNext').addEventListener('click',function(){
//     calMonth++; if(calMonth>12){calMonth=1;calYear++;} renderCalendar();
// });
//
// // ── 초기 렌더링 ──
// renderNotice(MOCK_NOTICES);
// renderCommunity(MOCK_COMMUNITY);
// renderTodaySchedule(MOCK_TODAY_SCHEDULES);
// renderAdminBilling(MOCK_BILLING);
// renderParking(MOCK_PARKED_COUNT, MOCK_PENDING_COUNT);
// renderReservation(MOCK_RESERVATIONS);
// renderInquiry(MOCK_INQUIRIES);
// renderComplaint(MOCK_COMPLAINTS);
// renderCalendar();