// Medical Record Model with Risk Assessment
export interface MedicalRecord {
  id?: number;
  userId: number;
  userName?: string;
  userEmail?: string;
  age: number;
  gender: 'Male' | 'Female' | 'Other';
  educationLevel: string;
  familyHistory: 'Yes' | 'No';
  riskFactors: string;
  currentSymptoms: string;
  diagnosisNotes: string;
  riskScore?: number;
  riskLevel?: RiskLevel;
  lastRiskCalculation?: string;
  hereditaryRiskContribution?: number;
  wellnessRiskContribution?: number;
  // ── Session 1 new fields ──
  apoeStatus?: string;
  apoeStatusDisplay?: string;
  diagnosisStage?: string;
  diagnosisStageDisplay?: string;
  diagnosisStageDescription?: string;
  stageManuallyOverridden?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export enum RiskLevel {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  CRITICAL = 'CRITICAL'
}

export interface MedicalRecordWithDetails extends MedicalRecord {
  riskFactorsList?: any[];
  preventionActions?: any[];
  timeline?: any[];
  recommendations?: any[];
}

export const EDUCATION_LEVELS = [
  'No Formal Education',
  'Primary School',
  'Middle School',
  'High School',
  'Vocational Training',
  'Some College',
  'Bachelor Degree',
  'Master Degree',
  'Doctorate'
] as const;

export type EducationLevel = typeof EDUCATION_LEVELS[number];

// APOE genotype options matching backend enum
export const APOE_STATUSES = [
  { value: 'NOT_TESTED',  label: 'Not Tested / Unknown',    riskContribution: 0  },
  { value: 'E2_E2',       label: 'E2/E2 — Strongly protective',  riskContribution: -8 },
  { value: 'E2_E3',       label: 'E2/E3 — Mildly protective',    riskContribution: -4 },
  { value: 'E3_E3',       label: 'E3/E3 — Average risk (most common)', riskContribution: 0 },
  { value: 'E2_E4',       label: 'E2/E4 — Slightly elevated',   riskContribution: 5  },
  { value: 'E3_E4',       label: 'E3/E4 — Elevated risk (1 copy E4)', riskContribution: 15 },
  { value: 'E4_E4',       label: 'E4/E4 — High risk (2 copies E4)',   riskContribution: 25 },
];

// Diagnosis stage options matching backend enum
export const DIAGNOSIS_STAGES = [
  { value: 'PRECLINICAL', label: 'Preclinical',   description: 'No symptoms, biological changes only' },
  { value: 'MCI',         label: 'MCI',           description: 'Mild Cognitive Impairment — noticeable but mild' },
  { value: 'MILD',        label: 'Mild',          description: 'Early dementia — daily life mostly unaffected' },
  { value: 'MODERATE',    label: 'Moderate',      description: 'Significant memory loss, needs assistance' },
  { value: 'SEVERE',      label: 'Severe',        description: 'Full care required, advanced stage' },
];
