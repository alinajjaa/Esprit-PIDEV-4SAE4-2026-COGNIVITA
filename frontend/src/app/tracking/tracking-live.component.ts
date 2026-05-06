import { ChangeDetectorRef, Component, NgZone, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, interval, of } from 'rxjs';
import { catchError, startWith, switchMap, takeUntil } from 'rxjs/operators';
import {
  GeofencePayload,
  GeofenceZone,
  TrackingAlert,
  TrackingLocation,
  TrackingService
} from '../services/tracking.service';

declare const google: any;

@Component({
  selector: 'app-tracking-live',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page">
      <div class="header">
        <h2>Live Patient Tracking</h2>
        <p>Map view with geofence radius controls and live motion updates</p>
      </div>

      <div class="error" *ngIf="error">{{ error }}</div>

      <div class="map-card">
        <div class="controls">
          <label>
            Patient
            <select [(ngModel)]="selectedPatientId" (ngModelChange)="onPatientChange()">
              <option [ngValue]="null">Select patient</option>
              <option *ngFor="let p of locations" [ngValue]="p.patientId">
                #{{ p.patientId }} ({{ p.motionState || 'unknown' }})
              </option>
            </select>
          </label>

          <label>
            Radius (meters)
            <input type="number" min="10" [(ngModel)]="radiusMeters">
          </label>

          <button (click)="saveGeofence()" [disabled]="selectedPatientId == null">Save Radius</button>
          <button class="secondary" (click)="deleteGeofence()" [disabled]="selectedPatientId == null">Delete Radius</button>
        </div>

        <div class="hint" *ngIf="!mapFallback">Tip: click on the map to move the geofence center for the selected patient.</div>
        <div class="hint" *ngIf="mapFallback">Google Maps key not configured. Showing coordinates and an OpenStreetMap link instead.</div>
        <div id="tracking-map" class="map" [class.hidden]="mapFallback"></div>
        <div class="map fallback-map" *ngIf="mapFallback">
          <div class="fallback-pin">Patient #{{ selectedPatientId || '-' }}</div>
          <div class="fallback-coords">
            <span>Latitude: {{ fallbackLatitude ?? 'N/A' }}</span>
            <span>Longitude: {{ fallbackLongitude ?? 'N/A' }}</span>
            <span>Radius: {{ radiusMeters }} m</span>
          </div>
          <a *ngIf="openStreetMapUrl" [href]="openStreetMapUrl" target="_blank" rel="noopener">Open map</a>
        </div>
      </div>

      <div class="table-wrap">
        <table *ngIf="locations.length > 0">
          <thead>
            <tr>
              <th>Patient</th>
              <th>Latitude</th>
              <th>Longitude</th>
              <th>Motion</th>
              <th>Timestamp (UTC)</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let item of locations" [class.selected]="item.patientId === selectedPatientId" (click)="selectFromTable(item.patientId)">
              <td>#{{ item.patientId }}</td>
              <td>{{ item.latitude | number:'1.6-6' }}</td>
              <td>{{ item.longitude | number:'1.6-6' }}</td>
              <td>
                <span class="badge" [class.stationary]="isStationary(item.motionState)" [class.moving]="!isStationary(item.motionState)">
                  {{ item.motionState || 'unknown' }}
                </span>
              </td>
              <td>{{ item.timestamp }}</td>
            </tr>
          </tbody>
        </table>
        <div class="empty" *ngIf="!error && locations.length === 0">No tracking data yet.</div>
      </div>

      <div class="alerts-card">
        <div class="alerts-head">
          <h3>Alerts</h3>
          <div class="alerts-actions">
            <label>
              <input type="checkbox" [(ngModel)]="showUnreadOnly" (change)="reloadAlerts()">
              Unread only
            </label>
          </div>
        </div>

        <div class="empty" *ngIf="alerts.length === 0">No alerts for current filter.</div>
        <div class="alert-row" *ngFor="let alert of alerts">
          <div class="left">
            <div class="type" [class.out]="alert.type === 'OUT_OF_ZONE'" [class.nomove]="alert.type === 'NO_MOVEMENT'">
              {{ alert.type }}
            </div>
            <div class="msg">{{ alert.message }}</div>
            <div class="meta">Patient #{{ alert.patientId }} · {{ alert.createdAt }}</div>
          </div>
          <button class="secondary" *ngIf="!alert.read" (click)="markAsRead(alert.id)">Mark as read</button>
          <span class="read" *ngIf="alert.read">Read</span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .page { padding: 24px; max-width: 1250px; margin: 0 auto; color: #e2e8f0; }
    .header h2 { margin: 0 0 6px; color: #fff; }
    .header p { margin: 0 0 14px; color: #93c5fd; }
    .error { color: #fca5a5; margin: 10px 0; }
    .map-card { background: rgba(255,255,255,0.04); border: 1px solid rgba(0,255,255,0.2); border-radius: 12px; padding: 12px; margin-bottom: 14px; }
    .controls { display: flex; flex-wrap: wrap; gap: 10px; align-items: end; }
    .controls label { display: grid; gap: 6px; color: #93c5fd; font-size: .85rem; }
    .controls select, .controls input { min-width: 180px; background: rgba(0,0,0,0.28); border: 1px solid rgba(0,255,255,0.2); color: #fff; border-radius: 8px; padding: 8px; }
    .controls button { background: #00bcd4; color: #041a20; border: none; border-radius: 8px; padding: 8px 12px; font-weight: 700; cursor: pointer; }
    .controls button.secondary { background: #334155; color: #e2e8f0; }
    .hint { color: #93c5fd; font-size: .82rem; margin: 10px 0; }
    .map { height: 430px; border-radius: 10px; overflow: hidden; border: 1px solid rgba(255,255,255,0.08); }
    .map.hidden { display: none; }
    .fallback-map { display: grid; place-items: center; align-content: center; gap: 14px; background: radial-gradient(circle at center, rgba(0,188,212,.18), rgba(15,23,42,.92)); color: #e2e8f0; }
    .fallback-pin { border: 1px solid rgba(0,255,255,0.35); border-radius: 999px; padding: 10px 16px; color: #fff; background: rgba(0,0,0,0.28); font-weight: 700; }
    .fallback-coords { display: flex; flex-wrap: wrap; justify-content: center; gap: 10px; color: #93c5fd; }
    .fallback-map a { color: #67e8f9; font-weight: 700; text-decoration: none; }
    .table-wrap { background: rgba(255,255,255,0.04); border: 1px solid rgba(0,255,255,0.2); border-radius: 12px; overflow: hidden; }
    table { width: 100%; border-collapse: collapse; }
    th, td { padding: 10px; border-bottom: 1px solid rgba(255,255,255,0.08); text-align: left; }
    th { color: #93c5fd; font-weight: 700; background: rgba(0,0,0,0.2); }
    td { color: #e2e8f0; }
    tr.selected { background: rgba(0,255,255,0.08); }
    .badge { display: inline-block; border-radius: 999px; padding: 4px 10px; font-size: .78rem; font-weight: 700; text-transform: lowercase; }
    .stationary { background: rgba(245, 158, 11, 0.2); color: #fbbf24; }
    .moving { background: rgba(16, 185, 129, 0.2); color: #34d399; }
    .empty { padding: 16px; color: #93c5fd; }
    .alerts-card { margin-top: 14px; background: rgba(255,255,255,0.04); border: 1px solid rgba(0,255,255,0.2); border-radius: 12px; padding: 12px; }
    .alerts-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
    .alerts-head h3 { margin: 0; color: #fff; }
    .alerts-actions { color: #93c5fd; font-size: .85rem; }
    .alert-row { border-top: 1px solid rgba(255,255,255,0.08); padding: 10px 0; display: flex; justify-content: space-between; gap: 10px; align-items: center; }
    .left { min-width: 0; }
    .type { display: inline-block; font-size: .72rem; padding: 3px 8px; border-radius: 999px; margin-bottom: 6px; font-weight: 700; }
    .type.out { background: rgba(248,113,113,.2); color: #f87171; }
    .type.nomove { background: rgba(250,204,21,.2); color: #facc15; }
    .msg { color: #e2e8f0; font-weight: 600; }
    .meta { color: #93c5fd; font-size: .8rem; margin-top: 3px; }
    .read { color: #34d399; font-weight: 700; font-size: .82rem; }
  `]
})
export class TrackingLiveComponent implements OnInit, OnDestroy {
  locations: TrackingLocation[] = [];
  alerts: TrackingAlert[] = [];
  error = '';
  selectedPatientId: number | null = null;
  radiusMeters = 100;
  showUnreadOnly = true;
  mapFallback = false;
  fallbackLatitude: number | null = null;
  fallbackLongitude: number | null = null;
  openStreetMapUrl = '';

  private readonly destroy$ = new Subject<void>();
  private map: any = null;
  private marker: any = null;
  private circle: any = null;
  private mapReady = false;
  private googleMapsKey = '';

  constructor(
    private readonly trackingService: TrackingService,
    private readonly ngZone: NgZone,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.googleMapsKey = (window as any).GOOGLE_MAPS_API_KEY || '';
    this.initMap();

    interval(2000)
      .pipe(
        startWith(0),
        switchMap(() => this.trackingService.getLatestLocations().pipe(catchError(() => of([] as TrackingLocation[])))),
        takeUntil(this.destroy$)
      )
      .subscribe(rows => {
        this.ngZone.run(() => {
          this.locations = rows || [];
          if (this.locations.length > 0 && this.selectedPatientId == null) {
            this.selectedPatientId = this.locations[0].patientId;
            this.onPatientChange();
          } else {
            this.syncMapWithSelection();
            this.reloadAlerts();
          }
          this.cdr.detectChanges();
        });
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  isStationary(state: string | null): boolean {
    return (state || '').toLowerCase() === 'stationary';
  }

  selectFromTable(patientId: number): void {
    this.selectedPatientId = patientId;
    this.onPatientChange();
  }

  onPatientChange(): void {
    if (this.selectedPatientId == null) return;
    this.loadGeofence(this.selectedPatientId);
    this.syncMapWithSelection();
    this.reloadAlerts();
  }

  saveGeofence(): void {
    const loc = this.getSelectedLocation();
    if (!loc || this.selectedPatientId == null) return;

    const center = this.circle?.getCenter?.();
    const payload: GeofencePayload = {
      patientId: this.selectedPatientId,
      centerLatitude: center ? center.lat() : loc.latitude,
      centerLongitude: center ? center.lng() : loc.longitude,
      radiusMeters: Number(this.radiusMeters) || 100
    };

    this.trackingService.upsertGeofence(payload).subscribe({
      next: () => {
        this.error = '';
        this.drawCircle(payload.centerLatitude, payload.centerLongitude, payload.radiusMeters);
      },
      error: () => {
        this.error = 'Failed to save geofence radius.';
      }
    });
  }

  deleteGeofence(): void {
    if (this.selectedPatientId == null) return;
    this.trackingService.deleteGeofence(this.selectedPatientId).subscribe({
      next: () => {
        this.error = '';
        if (this.circle) {
          this.circle.setMap(null);
          this.circle = null;
        }
      },
      error: () => {
        this.error = 'Failed to delete geofence radius.';
      }
    });
  }

  private loadGeofence(patientId: number): void {
    this.trackingService.getGeofence(patientId).subscribe({
      next: (zone: GeofenceZone) => {
        this.radiusMeters = zone.radiusMeters;
        this.drawCircle(zone.centerLatitude, zone.centerLongitude, zone.radiusMeters);
      },
      error: () => {
        const loc = this.getSelectedLocation();
        if (loc) {
          this.drawCircle(loc.latitude, loc.longitude, this.radiusMeters);
        }
      }
    });
  }

  private syncMapWithSelection(): void {
    const loc = this.getSelectedLocation();
    if (!loc) return;

    if (this.mapFallback || !this.mapReady) {
      this.updateFallbackMap(loc.latitude, loc.longitude);
      return;
    }

    const pos = { lat: loc.latitude, lng: loc.longitude };
    if (!this.marker) {
      this.marker = new google.maps.Marker({ map: this.map, position: pos, title: `Patient #${loc.patientId}` });
    } else {
      this.marker.setPosition(pos);
      this.marker.setTitle(`Patient #${loc.patientId}`);
    }
    this.map.panTo(pos);
  }

  private drawCircle(lat: number, lon: number, radius: number): void {
    if (this.mapFallback) {
      this.updateFallbackMap(lat, lon);
      return;
    }

    if (!this.mapReady) return;
    if (this.circle) this.circle.setMap(null);
    this.circle = new google.maps.Circle({
      map: this.map,
      center: { lat, lng: lon },
      radius,
      fillColor: '#00bcd4',
      fillOpacity: 0.18,
      strokeColor: '#00e5ff',
      strokeOpacity: 0.95,
      strokeWeight: 2
    });
  }

  private initMap(): void {
    if (!this.googleMapsKey && !(window as any).google?.maps) {
      this.mapFallback = true;
      this.updateFallbackMap(36.911339, 10.186115);
      return;
    }

    this.loadGoogleMapsScript().then(() => {
      this.map = new google.maps.Map(document.getElementById('tracking-map'), {
        center: { lat: 36.911339, lng: 10.186115 },
        zoom: 14,
        mapTypeControl: false,
        streetViewControl: false
      });
      this.map.addListener('click', (event: any) => {
        if (!event?.latLng || this.selectedPatientId == null) return;
        const lat = event.latLng.lat();
        const lon = event.latLng.lng();
        this.drawCircle(lat, lon, Number(this.radiusMeters) || 100);
      });
      this.mapReady = true;
      this.syncMapWithSelection();
    }).catch(() => {
      this.mapFallback = true;
      this.updateFallbackMap(36.911339, 10.186115);
    });
  }

  private loadGoogleMapsScript(): Promise<void> {
    if ((window as any).google?.maps) return Promise.resolve();

    if (!this.googleMapsKey) {
      return Promise.reject(new Error('Missing API key'));
    }

    return new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = `https://maps.googleapis.com/maps/api/js?key=${this.googleMapsKey}`;
      script.async = true;
      script.defer = true;
      script.onload = () => resolve();
      script.onerror = () => reject(new Error('Map script load failed'));
      document.head.appendChild(script);
    });
  }

  private getSelectedLocation(): TrackingLocation | undefined {
    return this.locations.find(x => x.patientId === this.selectedPatientId);
  }

  private updateFallbackMap(lat: number, lon: number): void {
    this.fallbackLatitude = Number(lat.toFixed(6));
    this.fallbackLongitude = Number(lon.toFixed(6));
    this.openStreetMapUrl = `https://www.openstreetmap.org/?mlat=${lat}&mlon=${lon}#map=16/${lat}/${lon}`;
  }

  reloadAlerts(): void {
    this.trackingService.getAlerts(this.selectedPatientId ?? undefined, this.showUnreadOnly).subscribe({
      next: (rows) => {
        this.ngZone.run(() => {
          this.alerts = rows || [];
          this.cdr.detectChanges();
        });
      },
      error: () => {
        this.ngZone.run(() => {
          this.alerts = [];
          this.cdr.detectChanges();
        });
      }
    });
  }

  markAsRead(alertId: number): void {
    this.trackingService.markAlertAsRead(alertId).subscribe({
      next: () => this.reloadAlerts(),
      error: () => {
        this.error = 'Failed to mark alert as read.';
      }
    });
  }
}
