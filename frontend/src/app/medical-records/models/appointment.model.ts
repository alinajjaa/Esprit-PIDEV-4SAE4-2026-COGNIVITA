export interface Appointment {
  id?: number;
  medicalRecordId?: number;
  doctorName?: string;
  specialty?: string;
  appointmentType: AppointmentType;
  scheduledAt: string;          // ISO datetime string
  status?: AppointmentStatus;
  location?: string;
  notes?: string;
  completedAt?: string;
  reminderSent?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export type AppointmentType =
  | 'GENERAL'
  | 'NEUROLOGIST'
  | 'MEMORY_CLINIC'
  | 'GENETICS'
  | 'PHARMACIST'
  | 'FOLLOW_UP'
  | 'IMAGING'
  | 'PSYCHIATRY';

export type AppointmentStatus =
  | 'SCHEDULED'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'MISSED'
  | 'RESCHEDULED';

export const APPOINTMENT_TYPES: { value: AppointmentType; label: string; icon: string }[] = [
  { value: 'GENERAL',       label: 'General Check-up',        icon: '🏥' },
  { value: 'NEUROLOGIST',   label: 'Neurologist',              icon: '🧠' },
  { value: 'MEMORY_CLINIC', label: 'Memory Clinic',            icon: '💭' },
  { value: 'GENETICS',      label: 'Genetics / APOE Testing',  icon: '🧬' },
  { value: 'PHARMACIST',    label: 'Pharmacist / Med Review',  icon: '💊' },
  { value: 'FOLLOW_UP',     label: 'Follow-up',                icon: '📋' },
  { value: 'IMAGING',       label: 'Brain Imaging (MRI/CT)',   icon: '🔬' },
  { value: 'PSYCHIATRY',    label: 'Psychiatry',               icon: '🩺' },
];

export const STATUS_CONFIG: Record<AppointmentStatus, { label: string; color: string; bg: string }> = {
  SCHEDULED:   { label: 'Scheduled',   color: '#2563eb', bg: '#eff6ff' },
  COMPLETED:   { label: 'Completed',   color: '#10b981', bg: '#f0fdf4' },
  CANCELLED:   { label: 'Cancelled',   color: '#94a3b8', bg: '#f8fafc' },
  MISSED:      { label: 'Missed',      color: '#dc2626', bg: '#fef2f2' },
  RESCHEDULED: { label: 'Rescheduled', color: '#f59e0b', bg: '#fffbeb' },
};

export interface AppointmentStats {
  total: number;
  scheduled: number;
  completed: number;
  missed: number;
  cancelled: number;
  attendanceRate: number;
}
