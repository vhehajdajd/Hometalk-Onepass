/**
 * HomeTalk OnePass — Notification SSE Client
 * path: /static/notification/js/notification-sse.js
 *
 * 사용법:
 *   const sse = new NotificationSSE({
 *     onMessage: (data) => { ... },
 *     onReconnect: () => { ... }
 *   });
 */
(function (window) {
    'use strict';

    // 컨텍스트 패스를 meta 태그에서 읽어옴 (HTML에 <meta name="ctx" content="/hometop"> 필수)
    const CTX = document.querySelector('meta[name="ctx"]')?.content || '';

    class NotificationSSE {
        constructor(options = {}) {
            this.onMessage   = options.onMessage   || (() => {});
            this.onReconnect = options.onReconnect || (() => {});
            this.onConnect   = options.onConnect   || (() => {});

            this.eventSource    = null;
            this.lastEventId    = null;
            this.reconnectTimer = null;
            this.retryCount     = 0;
            this.maxRetry       = 5;

            this.connect();
        }

        connect() {
            if (this.eventSource) {
                this.eventSource.close();
            }

            const url = `${CTX}/api/notification/subscribe`;
            this.eventSource = new EventSource(url, { withCredentials: true });

            // 최초 연결 확인
            this.eventSource.addEventListener('connect', (e) => {
                console.log('[SSE] connected');
                this.retryCount = 0;
                this.onConnect(e);
            });

            // 알림 수신
            this.eventSource.addEventListener('notification', (e) => {
                this.lastEventId = e.lastEventId;
                try {
                    const data = JSON.parse(e.data);
                    this.onMessage(data);
                } catch (err) {
                    console.error('[SSE] parse error:', err);
                }
            });

            // 서버 측 재연결 시그널 (onTimeout 발생 시 서버에서 발송)
            this.eventSource.addEventListener('reconnect', () => {
                console.log('[SSE] server requested reconnect');
                this.eventSource.close();
                this.scheduleReconnect();
            });

            // 네트워크 단절 등
            this.eventSource.onerror = (e) => {
                console.warn('[SSE] error, will reconnect');
                this.eventSource.close();
                this.scheduleReconnect();
            };
        }

        scheduleReconnect() {
            if (this.retryCount >= this.maxRetry) {
                console.error('[SSE] max retry reached');
                return;
            }

            // 지수 백오프 (1s, 2s, 4s, 8s, 16s)
            const delay = Math.min(1000 * Math.pow(2, this.retryCount), 16000);
            this.retryCount++;

            clearTimeout(this.reconnectTimer);
            this.reconnectTimer = setTimeout(() => {
                this.onReconnect();
                this.connect();
            }, delay);
        }

        close() {
            clearTimeout(this.reconnectTimer);
            if (this.eventSource) this.eventSource.close();
        }
    }

    // 전역 노출
    window.NotificationSSE = NotificationSSE;
    window.NOTI_CTX = CTX;
})(window);