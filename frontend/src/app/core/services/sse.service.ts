import { Injectable, NgZone, OnDestroy } from '@angular/core';
import { Observable, Subject } from 'rxjs';

export interface SseEvent {
  channel: string;
}

const SSE_URL = (typeof window !== 'undefined' && window.location.hostname === 'localhost' && window.location.port === '4200')
  ? 'http://localhost:8080/api/sse/subscribe'
  : '/api/sse/subscribe';

@Injectable({ providedIn: 'root' })
export class SseService implements OnDestroy {
  private eventSubject = new Subject<SseEvent>();
  private eventSource: EventSource | null = null;
  private reconnectDelay = 1000;
  private reconnectTimer: any = null;

  events$: Observable<SseEvent> = this.eventSubject.asObservable();

  constructor(private ngZone: NgZone) {
    this.connect();
  }

  private connect(): void {
    if (typeof window === 'undefined' || typeof EventSource === 'undefined') return;

    try {
      this.eventSource = new EventSource(SSE_URL);

      this.eventSource.addEventListener('data_changed', (event: any) => {
        try {
          const data = JSON.parse(event.data) as SseEvent;
          this.ngZone.run(() => this.eventSubject.next(data));
        } catch (e) {
          // ignore parse errors
        }
      });

      this.eventSource.onerror = () => {
        this.eventSource?.close();
        this.eventSource = null;
        this.scheduleReconnect();
      };

      this.eventSource.onopen = () => {
        this.reconnectDelay = 1000;
      };
    } catch (e) {
      this.scheduleReconnect();
    }
  }

  private scheduleReconnect(): void {
    if (this.reconnectTimer) return;
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      this.connect();
    }, this.reconnectDelay);
    this.reconnectDelay = Math.min(this.reconnectDelay * 2, 30000);
  }

  ngOnDestroy(): void {
    this.eventSource?.close();
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer);
  }
}
