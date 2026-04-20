import { API } from '../core/api';
// rendezvous.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';

// FullCalendar
import { FullCalendarModule } from '@fullcalendar/angular';
import { CalendarOptions } from '@fullcalendar/core';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';

type RendezVousStatus = 'PLANIFIE' | 'CONFIRME' | 'ANNULE' | 'TERMINE';

type MedecinOption = {
  id: number;
  nomMedecin: string;
};

type ToastType = 'success' | 'error' | 'info' | 'warn';

type Toast = {
  id: number;
  type: ToastType;
  title: string;
  message: string;
  durationMs: number;
  closing?: boolean;
};

// DTO returned by /calendar endpoint
type RendezVousCalendarDto = {
  id: number;
  dateHeure: string; // ISO string
  status?: string;
  patientName?: string;
  chambre?: string;
};

// DTO returned by /next-available
type SlotDto = {
  start: string; // ISO string
  end: string;   // ISO string
};

@Component({
  selector: 'app-rendezvous',
  standalone: true,
  imports: [CommonModule, FormsModule, HttpClientModule, RouterLink, FullCalendarModule],
  templateUrl: './rendezvous.html',
  styleUrls: ['./rendezvous.css'],
})
export class Rendezvous implements OnInit {
  private rdvApiUrl = API.RDV_API;
  private medecinApiUrl = API.MEDECIN_API;

  // for now fixed patient id = 1
  patientId: number = 1;

  medecinId: number | null = null;
  dateHeure = '';
  status: RendezVousStatus = 'PLANIFIE';

  minDateTime: string = '';
  loading = false;

  medecinOptions: MedecinOption[] = [];

  // Calendar
  private busyEvents: { start: Date; end: Date }[] = [];
  private slotMinutes = 30;

  showCalendar = false;
  calendarLoading = false;

  // Suggestions (next available)
  urgentMode = true;
  suggestions: SlotDto[] = [];
  suggestionsLoading = false;

  calendarOptions: CalendarOptions = {
    plugins: [timeGridPlugin, interactionPlugin],
    initialView: 'timeGridWeek',
    nowIndicator: true,
    allDaySlot: false,
    height: 'auto',
    slotMinTime: '08:00:00',
    slotMaxTime: '18:00:00',
    selectable: true,
    selectMirror: true,

    headerToolbar: {
      left: 'prev,next today',
      center: 'title',
      right: 'timeGridDay,timeGridWeek',
    },

    eventTimeFormat: { hour: '2-digit', minute: '2-digit', hour12: false },
    events: [],

    // When navigating, regenerate free slots for the visible range + remove focus ring
    datesSet: (arg) => {
      setTimeout(() => (document.activeElement as HTMLElement | null)?.blur(), 0);
      this.refreshFreeSlots(arg.start, arg.end);
    },

    // Click empty time -> autofill if free
    dateClick: (info) => {
      if (!this.medecinId) return;

      const clicked = info.date;
      if (!this.isWithinWorkingHours(clicked)) return;

      const end = new Date(clicked.getTime() + this.slotMinutes * 60 * 1000);
      if (this.overlapsBusy(clicked, end)) {
        this.notify('warn', 'Indisponible', 'Ce créneau est déjà réservé.');
        return;
      }

      this.dateHeure = this.toDatetimeLocal(clicked);
      this.notify('info', 'Créneau sélectionné', 'Date/heure remplie automatiquement.');
    },

    // Prevent selecting busy ranges by dragging
    selectAllow: (selectInfo) => {
      if (!this.isWithinWorkingHours(selectInfo.start)) return false;
      if (!this.isWithinWorkingHours(new Date(selectInfo.end.getTime() - 1))) return false;
      return !this.overlapsBusy(selectInfo.start, selectInfo.end);
    },

    // Drag selection -> autofill too
    select: (selectInfo) => {
      this.dateHeure = this.toDatetimeLocal(selectInfo.start);
    },
  };

  // Toasts
  toasts: Toast[] = [];
  private toastId = 1;

  constructor(private http: HttpClient, private router: Router) {}

  ngOnInit(): void {
    this.setMinDateTime();
    this.loadMedecinOptions();
  }

  // =========================
  //  TOAST HELPERS
  // =========================
  notify(type: ToastType, title: string, message: string, durationMs: number = 3500): void {
    const id = this.toastId++;
    const toast: Toast = { id, type, title, message, durationMs, closing: false };
    this.toasts = [toast, ...this.toasts].slice(0, 4);

    if (durationMs > 0) setTimeout(() => this.dismissToast(id), durationMs);
  }

  dismissToast(id: number): void {
    const t = this.toasts.find((x) => x.id === id);
    if (!t) return;

    t.closing = true;
    setTimeout(() => {
      this.toasts = this.toasts.filter((x) => x.id !== id);
    }, 180);
  }

  setMinDateTime(): void {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    this.minDateTime = `${year}-${month}-${day}T${hours}:${minutes}`;
  }

  loadMedecinOptions(): void {
    this.http.get<MedecinOption[]>(`${this.medecinApiUrl}/options`).subscribe({
      next: (data) => (this.medecinOptions = [...data]),
      error: () => this.notify('error', 'Erreur', 'Impossible de charger la liste des médecins.'),
    });
  }

  // =========================
  //  DOCTOR SELECT -> calendar + suggestions
  // =========================
  onMedecinChange(id: number | null): void {
    this.medecinId = id;

    if (!this.medecinId) {
      this.showCalendar = false;
      this.calendarOptions = { ...this.calendarOptions, events: [] };
      this.busyEvents = [];
      this.suggestions = [];
      return;
    }

    this.showCalendar = true;
    this.loadMedecinCalendar(this.medecinId);
    this.loadNextAvailableSlots(this.medecinId);
  }

  // =========================
  //  CALENDAR LOAD (busy only, free background generated client-side)
  // =========================
  private buildTitle(r: RendezVousCalendarDto): string {
    const status = (r.status ?? '').toUpperCase();
    const patient = r.patientName ? `Patient: ${r.patientName}` : `RDV #${r.id}`;
    const chambre = r.chambre ? ` • ${r.chambre}` : '';
    return status ? `${patient}${chambre} (${status})` : `${patient}${chambre}`;
  }

  loadMedecinCalendar(medecinId: number): void {
    this.calendarLoading = true;

    this.http.get<RendezVousCalendarDto[]>(`${this.rdvApiUrl}/medecin/${medecinId}/calendar`).subscribe({
      next: (rdvs) => {
        const now = new Date();

        // busy events only (future)
        const busy = rdvs
          .filter((r) => new Date(r.dateHeure) >= now)
          .map((r) => {
            const start = new Date(r.dateHeure);
            const end = new Date(start.getTime() + this.slotMinutes * 60 * 1000);

            return {
              id: String(r.id),
              title: this.buildTitle(r),
              start: start.toISOString(),
              end: end.toISOString(),
            };
          });

        this.busyEvents = busy.map((e) => ({
          start: new Date(e.start as string),
          end: new Date(e.end as string),
        }));

        // Set calendar events = busy only
        this.calendarOptions = { ...this.calendarOptions, events: busy };

        this.calendarLoading = false;
      },
      error: () => {
        this.calendarLoading = false;
        this.notify('error', 'Erreur', 'Impossible de charger le planning du médecin.');
        this.calendarOptions = { ...this.calendarOptions, events: [] };
        this.busyEvents = [];
      },
    });
  }

  // =========================
  //  NEXT AVAILABLE (3 slots)
  // =========================
  onUrgentToggle(): void {
    if (!this.medecinId) return;
    this.loadNextAvailableSlots(this.medecinId);
  }

  loadNextAvailableSlots(medecinId: number): void {
    this.suggestionsLoading = true;

    const from = this.urgentMode
      ? new Date().toISOString()
      : this.dateHeure
        ? new Date(this.dateHeure).toISOString()
        : new Date().toISOString();

    this.http
      .get<SlotDto[]>(`${this.rdvApiUrl}/medecin/${medecinId}/next-available`, {
        params: { count: 3, from },
      })
      .subscribe({
        next: (data) => {
          this.suggestions = Array.isArray(data) ? data : [];
          this.suggestionsLoading = false;
        },
        error: () => {
          this.suggestionsLoading = false;
          this.suggestions = [];
          this.notify('error', 'Erreur', 'Impossible de charger les prochains créneaux.');
        },
      });
  }

  selectSuggestedSlot(slot: SlotDto): void {
    this.dateHeure = this.toDatetimeLocal(new Date(slot.start));
    this.notify('info', 'Créneau choisi', 'Date/heure remplie automatiquement.');
  }

  // =========================
  //  RESERVATION FLOW
  // =========================
  reserver(): void {
    if (!this.medecinId || !this.dateHeure) {
      this.notify('warn', 'Champs requis', 'Choisissez un médecin et une date/heure.');
      return;
    }

    const selectedDate = new Date(this.dateHeure);
    const hour = selectedDate.getHours();
    if (hour < 8 || hour >= 17) {
      this.notify('warn', 'Horaire invalide', 'Disponibles uniquement entre 8h et 17h.');
      return;
    }

    const docName = this.getMedecinName(this.medecinId) || `#${this.medecinId}`;
    const prettyDate = selectedDate.toLocaleString();

    const ok = confirm(`Confirmer le rendez-vous ?\n\nMédecin: ${docName}\nDate: ${prettyDate}`);
    if (!ok) return;

    const payload = {
      dateHeure: this.dateHeure,
      status: 'PLANIFIE',
      medecin: { id: this.medecinId },
      patient: { id: this.patientId },
    };

    this.loading = true;

    this.http.post<any>(this.rdvApiUrl, payload).subscribe({
      next: () => {
        this.loading = false;
        this.notify('success', 'Rendez-vous réservé', `Avec ${docName} — ${prettyDate}`);

        // Refresh doctor calendar + suggestions
        this.loadMedecinCalendar(this.medecinId!);
        this.loadNextAvailableSlots(this.medecinId!);

        // Reset only date (keep doctor)
        this.dateHeure = '';

        this.router.navigateByUrl('/rendezvousList');
      },
      error: (err) => {
        this.loading = false;
        const msg = err?.error?.message || err?.error || 'Veuillez réessayer.';
        this.notify('error', 'Échec réservation', msg);

        // If conflict, refresh suggestions (someone may have booked)
        this.medecinId && this.loadNextAvailableSlots(this.medecinId);
      },
    });
  }

  getMedecinName(medecinId?: number): string {
    if (!medecinId) return '';
    const found = this.medecinOptions.find((m) => m.id === medecinId);
    return found ? found.nomMedecin : '';
  }

  // =========================
  //  FREE SLOTS BACKGROUND (client)
  // =========================
  private overlapsBusy(start: Date, end: Date): boolean {
    return this.busyEvents.some((b) => start < b.end && end > b.start);
  }

  private isWithinWorkingHours(d: Date): boolean {
    const h = d.getHours();
    const m = d.getMinutes();
    const minutes = h * 60 + m;
    const start = 8 * 60; // 08:00
    const end = 17 * 60; // 17:00
    return minutes >= start && minutes < end;
  }

  private refreshFreeSlots(rangeStart: Date, rangeEnd: Date): void {
    const current = this.calendarOptions.events as any[];
    if (!Array.isArray(current)) return;

    const busyOnly = current.filter((e) => !(e.extendedProps && e.extendedProps.kind === 'FREE'));

    const freeSlots: any[] = [];
    const day = new Date(rangeStart);

    while (day < rangeEnd) {
      const date = new Date(day);

      const startDay = new Date(date);
      startDay.setHours(8, 0, 0, 0);

      const endDay = new Date(date);
      endDay.setHours(17, 0, 0, 0);

      for (
        let t = new Date(startDay);
        t < endDay;
        t = new Date(t.getTime() + this.slotMinutes * 60 * 1000)
      ) {
        const slotStart = new Date(t);
        const slotEnd = new Date(t.getTime() + this.slotMinutes * 60 * 1000);

        if (!this.overlapsBusy(slotStart, slotEnd)) {
          freeSlots.push({
            start: slotStart.toISOString(),
            end: slotEnd.toISOString(),
            display: 'background',
            classNames: ['free-slot-bg'],
            extendedProps: { kind: 'FREE' },
          });
        }
      }

      day.setDate(day.getDate() + 1);
      day.setHours(0, 0, 0, 0);
    }

    this.calendarOptions = { ...this.calendarOptions, events: [...busyOnly, ...freeSlots] };
  }

  private toDatetimeLocal(d: Date): string {
    const pad = (n: number) => String(n).padStart(2, '0');
    const yyyy = d.getFullYear();
    const mm = pad(d.getMonth() + 1);
    const dd = pad(d.getDate());
    const hh = pad(d.getHours());
    const min = pad(d.getMinutes());
    return `${yyyy}-${mm}-${dd}T${hh}:${min}`;
  }
}
