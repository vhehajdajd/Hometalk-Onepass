const CTX = '';

window.IS_LOGGED_IN = false;

const SERVICE_URLS = {
  notice:    CTX + '/hometop/notice',
  schedule:  CTX + '/hometop/schedule',
  billing:   CTX + '/hometop/billing',
  parking:   CTX + '/hometop/parking/visit',
  community: CTX + '/hometop/community/square/all',
  civil:     CTX + '/hometop/civil',
  facility:  CTX + '/hometop/reservation'
};

const API_ENDPOINTS = {
  notice:    CTX + '/hometop/notice/api/detail',
  schedule:  CTX + '/hometop/schedule/api/calendar',
  community: CTX + '/hometop/api/community/recent'
};

function goService(serviceKey) {
  const destination = SERVICE_URLS[serviceKey] || SERVICE_URLS['community'];
  if (window.IS_LOGGED_IN) {
    location.href = destination;
  } else {
    showToast('로그인이 필요한 서비스입니다.\n로그인 페이지로 이동합니다.');
    setTimeout(function () {
      location.href = CTX + '/hometop/auth?redirectURL=' + encodeURIComponent(destination);
    }, 1200);
  }
}

function goCommunity(boardCode) {
  const destination = CTX + '/hometop/community/' + encodeURIComponent(boardCode) + '/all';
  if (window.IS_LOGGED_IN) {
    location.href = destination;
  } else {
    showToast('로그인이 필요한 서비스입니다.\n로그인 페이지로 이동합니다.');
    setTimeout(function () {
      location.href = CTX + '/hometop/auth?redirectURL=' + encodeURIComponent(destination);
    }, 1200);
  }
}

async function initHome() {
  await Promise.allSettled([
    loadSection(API_ENDPOINTS.notice,    renderNotices,   'notice-list',    '공지사항'),
    loadSection(API_ENDPOINTS.schedule,  renderSchedule,  'schedule-list',  '일정'),
    loadSection(API_ENDPOINTS.community, renderCommunity, 'community-list', '커뮤니티')
  ]);

  if (!window.IS_LOGGED_IN) {
    const prompt = document.getElementById('login-prompt');
    if (prompt) prompt.style.display = '';
  }
}

async function loadSection(url, renderFn, elId, label) {
  try {
    const res = await fetch(url);
    if (!res.ok) throw new Error('HTTP ' + res.status);
    const data = await res.json();
    renderFn(data);
  } catch (err) {
    console.error('[' + label + '] 로드 실패:', err);
    showErrorState(elId, label, url, renderFn);
  }
}

function showErrorState(elId, label, url, renderFn) {
  const el = document.getElementById(elId);
  if (!el) return;

  const retryFnName = '_retry_' + elId.replace(/-/g, '_');
  window[retryFnName] = function () {
    el.innerHTML =
      '<li><div class="skeleton-wrap">' +
        '<div class="skeleton-line long"></div>' +
        '<div class="skeleton-line medium"></div>' +
      '</div></li>';
    loadSection(url, renderFn, elId, label);
  };

  el.innerHTML =
    '<li class="error-state">' +
      '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">' +
        '<circle cx="12" cy="12" r="10"/>' +
        '<line x1="12" y1="8" x2="12" y2="12"/>' +
        '<line x1="12" y1="16" x2="12.01" y2="16"/>' +
      '</svg>' +
      '<p>' + escHtml(label) + ' 정보를 불러오지 못했습니다.</p>' +
      '<button class="retry-btn" onclick="' + retryFnName + '()">다시 시도</button>' +
    '</li>';
}

function renderNotices(list) {
  const el = document.getElementById('notice-list');
  if (!el) return;
  if (!list || list.length === 0) {
    el.innerHTML = '<li class="notice-empty">등록된 공지사항이 없습니다.</li>';
    return;
  }
  const BADGE_LABEL = { safety: '안전', facility: '시설', urgent: '긴급', normal: '일반', notice: '공지' };
  el.innerHTML = list.map(function (n) {
    const pin      = n.isPinned ? '📌 ' : '';
    const badgeKey = String(n.badge || 'notice').toLowerCase();
    const badgeTxt = BADGE_LABEL[badgeKey] || '공지';
    return (
      '<li class="notice-item" onclick="goService(\'notice\')">' +
        '<span class="badge badge-' + badgeKey + '">' + badgeTxt + '</span>' +
        '<span class="notice-title">' + pin + escHtml(n.title) + '</span>' +
        '<span class="notice-date">' + formatDate(n.createdAt) + '</span>' +
      '</li>'
    );
  }).join('');
}

function renderSchedule(list) {
  const el = document.getElementById('schedule-list');
  if (!el) return;
  if (!list || list.length === 0) {
    el.innerHTML =
      '<li class="schedule-empty">' +
        '<svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">' +
          '<rect x="3" y="4" width="18" height="18" rx="2"/>' +
          '<line x1="16" y1="2" x2="16" y2="6"/>' +
          '<line x1="8" y1="2" x2="8" y2="6"/>' +
          '<line x1="3" y1="10" x2="21" y2="10"/>' +
        '</svg>' +
        '<p>오늘 등록된 일정이 없습니다.</p>' +
        '<p class="sub">편안한 하루 보내세요!</p>' +
      '</li>';
    return;
  }
  el.innerHTML = list.map(function (s) {
    const dotClass = 'dot-' + String(s.badge || 'DEFAULT').toUpperCase();
    const timeStr  = formatTimeRange(s.startAt, s.endAt);
    return (
      '<li class="schedule-item" onclick="goService(\'schedule\')">' +
        '<span class="schedule-dot ' + dotClass + '"></span>' +
        '<div class="schedule-info">' +
          '<span class="schedule-title">' + escHtml(s.title) + '</span>' +
          (timeStr ? '<span class="schedule-time">' + timeStr + '</span>' : '') +
        '</div>' +
      '</li>'
    );
  }).join('');
}

function renderCommunity(list) {
  const el = document.getElementById('community-list');
  if (!el) return;
  if (!list || list.length === 0) {
    el.innerHTML = '<li class="community-empty">최신 게시글이 없습니다.</li>';
    return;
  }
  el.innerHTML = list.map(function (c) {
    const boardCode = escHtml(String(c.boardCode || 'square'));
    const category  = escHtml(c.categoryName || '일반');
    return (
      '<li class="community-item" onclick="goCommunity(\'' + boardCode + '\')">' +
        '<span class="community-cat">[' + category + ']</span>' +
        '<span class="community-title">' + escHtml(c.title) + '</span>' +
      '</li>'
    );
  }).join('');
}

function showToast(msg) {
  const t = document.getElementById('toast');
  if (!t) return;
  t.textContent = msg;
  t.classList.add('show');
  clearTimeout(t._timer);
  t._timer = setTimeout(function () { t.classList.remove('show'); }, 2800);
}

function formatDate(isoStr) {
  if (!isoStr) return '';
  const d = new Date(isoStr);
  if (isNaN(d.getTime())) return '';
  return d.getFullYear() + '.' +
    String(d.getMonth() + 1).padStart(2, '0') + '.' +
    String(d.getDate()).padStart(2, '0');
}

function formatTime(isoStr) {
  if (!isoStr) return '';
  const d = new Date(isoStr);
  if (isNaN(d.getTime())) return '';
  return String(d.getHours()).padStart(2, '0') + ':' +
    String(d.getMinutes()).padStart(2, '0');
}

function formatTimeRange(startAt, endAt) {
  const start = formatTime(startAt);
  if (!start) return '';
  const end = formatTime(endAt);
  return end ? start + ' ~ ' + end : start;
}

function escHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

document.addEventListener('DOMContentLoaded', initHome);