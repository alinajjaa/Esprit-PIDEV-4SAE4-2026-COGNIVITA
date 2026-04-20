import { Component, Input, OnInit, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RiskFactorService } from '../../services/risk-factor.service';
import { RiskFactor } from '../../models/risk-factor.model';

export interface RiskFactorCategory {
  name: string; icon: string; color: string;
  factors: RiskFactorDefinition[];
}
export interface RiskFactorDefinition {
  type: string; label: string; description: string;
  alzheimerLink: string; defaultSeverity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
}

export const RISK_FACTOR_CATEGORIES: RiskFactorCategory[] = [
  {
    name: 'Cardiovascular & Metabolic', icon: '🫀', color: '#ef4444',
    factors: [
      { type: 'Hypertension', label: 'Hypertension (High Blood Pressure)', description: 'Systolic BP ≥ 140 mmHg or diastolic ≥ 90 mmHg', alzheimerLink: 'Damages cerebral blood vessels; midlife hypertension doubles Alzheimer\'s risk', defaultSeverity: 'HIGH' },
      { type: 'Diabetes Type 2', label: 'Type 2 Diabetes', description: 'Fasting glucose ≥ 126 mg/dL or HbA1c ≥ 6.5%', alzheimerLink: 'Insulin resistance impairs brain glucose metabolism; increases risk by 65%', defaultSeverity: 'HIGH' },
      { type: 'Cardiovascular Disease', label: 'Cardiovascular Disease', description: 'Coronary artery disease, heart failure, atrial fibrillation', alzheimerLink: 'Reduces cerebral blood flow and oxygen delivery to brain tissue', defaultSeverity: 'CRITICAL' },
      { type: 'High Cholesterol', label: 'Dyslipidemia (High Cholesterol)', description: 'LDL ≥ 160 mg/dL or total cholesterol ≥ 240 mg/dL', alzheimerLink: 'High LDL promotes amyloid plaque formation', defaultSeverity: 'MEDIUM' },
      { type: 'Obesity', label: 'Obesity / Overweight', description: 'BMI ≥ 30 (obese) or 25–29.9 (overweight)', alzheimerLink: 'Adipose tissue promotes neuroinflammation; midlife obesity increases dementia risk 42%', defaultSeverity: 'MEDIUM' },
      { type: 'Stroke History', label: 'Prior Stroke / TIA', description: 'Ischemic stroke, hemorrhagic stroke, or transient ischemic attack', alzheimerLink: 'Direct brain tissue damage; vascular dementia and Alzheimer\'s frequently co-occur', defaultSeverity: 'CRITICAL' },
    ]
  },
  {
    name: 'Lifestyle & Behavioral', icon: '🧬', color: '#f59e0b',
    factors: [
      { type: 'Smoking', label: 'Tobacco Smoking', description: 'Current or former smoker (pack-years relevant)', alzheimerLink: 'Vascular damage and oxidative stress; increases risk ~80%', defaultSeverity: 'HIGH' },
      { type: 'Sedentary Lifestyle', label: 'Physical Inactivity', description: 'Less than 150 min/week moderate aerobic exercise', alzheimerLink: 'Exercise promotes BDNF and neurogenesis; inactivity is major modifiable risk', defaultSeverity: 'MEDIUM' },
      { type: 'Poor Diet', label: 'Poor Dietary Habits', description: 'High processed foods, low Mediterranean diet adherence', alzheimerLink: 'MIND diet reduces Alzheimer\'s risk by up to 35-53%', defaultSeverity: 'MEDIUM' },
      { type: 'Alcohol Abuse', label: 'Excessive Alcohol Consumption', description: '> 14 drinks/week men, > 7 drinks/week women', alzheimerLink: 'Neurotoxic; chronic heavy use accelerates brain atrophy', defaultSeverity: 'HIGH' },
      { type: 'Social Isolation', label: 'Social Isolation & Loneliness', description: 'Limited social contact, few meaningful relationships', alzheimerLink: 'Isolation associated with 26% increased dementia risk', defaultSeverity: 'MEDIUM' },
    ]
  },
  {
    name: 'Neurological & Psychiatric', icon: '🧠', color: '#8b5cf6',
    factors: [
      { type: 'Depression', label: 'Depression / Major Depressive Disorder', description: 'Clinically diagnosed depression; PHQ-9 ≥ 10', alzheimerLink: 'Elevated cortisol damages hippocampus; may be prodromal or causal', defaultSeverity: 'HIGH' },
      { type: 'Head Trauma', label: 'Traumatic Brain Injury (TBI)', description: 'Prior moderate-severe head injury, concussions, loss of consciousness', alzheimerLink: 'Disrupts blood-brain barrier; accelerates tau and amyloid pathology', defaultSeverity: 'CRITICAL' },
      { type: 'Sleep Disorders', label: 'Sleep Disorders / Insomnia', description: 'Sleep apnea, chronic insomnia, < 6 hrs/night', alzheimerLink: 'Sleep critical for amyloid clearance via glymphatic system', defaultSeverity: 'MEDIUM' },
      { type: 'Anxiety Disorder', label: 'Anxiety / PTSD / Chronic Stress', description: 'GAD, PTSD, or chronic stress with anxiety symptoms', alzheimerLink: 'Elevated cortisol causes hippocampal atrophy and synaptic loss', defaultSeverity: 'LOW' },
    ]
  },
  {
    name: 'Sensory & Cognitive', icon: '👁️', color: '#06b6d4',
    factors: [
      { type: 'Hearing Loss', label: 'Hearing Loss (Untreated)', description: 'Moderate to severe hearing impairment without hearing aids', alzheimerLink: 'Strongest modifiable risk factor; untreated raises risk up to 91%', defaultSeverity: 'HIGH' },
      { type: 'Vision Problems', label: 'Uncorrected Vision Impairment', description: 'Significant vision loss not corrected by glasses/surgery', alzheimerLink: 'Sensory deprivation reduces cognitive stimulation; linked to 26% increased risk', defaultSeverity: 'LOW' },
      { type: 'Cognitive Decline MCI', label: 'Mild Cognitive Impairment (MCI)', description: 'Memory/thinking concerns beyond normal aging', alzheimerLink: 'MCI progresses to Alzheimer\'s in 10-15% per year; critical early window', defaultSeverity: 'CRITICAL' },
    ]
  },
  {
    name: 'Medical Conditions', icon: '💊', color: '#10b981',
    factors: [
      { type: 'Chronic Kidney Disease', label: 'Chronic Kidney Disease (CKD)', description: 'eGFR < 60 mL/min/1.73m² for > 3 months', alzheimerLink: 'Uremic toxins cross blood-brain barrier; accelerated cognitive decline', defaultSeverity: 'MEDIUM' },
      { type: 'Thyroid Disorder', label: 'Thyroid Dysfunction', description: 'Hypothyroidism or hyperthyroidism (untreated)', alzheimerLink: 'Thyroid hormones essential for neuronal function', defaultSeverity: 'LOW' },
      { type: 'Autoimmune Disease', label: 'Autoimmune / Inflammatory Disease', description: 'Rheumatoid arthritis, lupus, MS, IBD', alzheimerLink: 'Systemic inflammation elevates brain cytokines promoting neurodegeneration', defaultSeverity: 'MEDIUM' },
    ]
  }
];

@Component({
  selector: 'app-risk-factors',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './risk-factors.component.html',
  styleUrls: ['./risk-factors.component.css']
})
export class RiskFactorsComponent implements OnInit, OnChanges {
  @Input() medicalRecordId!: number;

  riskFactors: RiskFactor[] = [];
  loading = false;
  error: string | null = null;
  showForm = false;
  isEditing = false;
  showCategoryPicker = false;
  selectedCategory: RiskFactorCategory | null = null;
  selectedFactorDef: RiskFactorDefinition | null = null;

  formData: Partial<RiskFactor> = { factorType: '', severity: 'MEDIUM', notes: '', isActive: true, diagnosedDate: '' };
  clinicalDetails = { controlStatus: '', onMedication: false, medicationDetails: '', lastLabValue: '', monitoring: '' };

  categories = RISK_FACTOR_CATEGORIES;
  severityLevels: Array<'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'> = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
  todayDate = new Date().toISOString().split('T')[0];
  activeFilter = 'ALL';

  stats = { totalCount: 0, activeCount: 0, inactiveCount: 0, criticalCount: 0, highCount: 0 };

  constructor(private riskFactorService: RiskFactorService) {}
  ngOnInit(): void { this.load(); }
  ngOnChanges(): void { if (this.medicalRecordId) this.load(); }

  load(): void { this.loadRiskFactors(); this.loadStats(); }

  loadRiskFactors(): void {
    if (!this.medicalRecordId) return;
    this.loading = true;
    this.riskFactorService.getRiskFactorsByMedicalRecord(this.medicalRecordId, 0, 50).subscribe({
      next: (r) => { this.riskFactors = r.data.content || []; this.loading = false; },
      error: () => { this.loading = false; this.error = 'Failed to load risk factors'; }
    });
  }

  loadStats(): void {
    if (!this.medicalRecordId) return;
    this.riskFactorService.getRiskFactorStats(this.medicalRecordId).subscribe({
      next: (r) => { this.stats = { ...this.stats, ...r.data }; }
    });
  }

  get filteredFactors(): RiskFactor[] {
    if (this.activeFilter === 'ALL') return this.riskFactors;
    if (this.activeFilter === 'ACTIVE') return this.riskFactors.filter(f => f.isActive);
    if (this.activeFilter === 'INACTIVE') return this.riskFactors.filter(f => !f.isActive);
    return this.riskFactors.filter(f => f.severity === this.activeFilter);
  }

  openAddForm(): void { this.showCategoryPicker = true; this.isEditing = false; this.error = null; }

  selectFactor(cat: RiskFactorCategory, def: RiskFactorDefinition): void {
    this.selectedCategory = cat;
    this.selectedFactorDef = def;
    this.clinicalDetails = { controlStatus: '', onMedication: false, medicationDetails: '', lastLabValue: '', monitoring: '' };
    this.formData = { medicalRecordId: this.medicalRecordId, factorType: def.type, severity: def.defaultSeverity, notes: '', isActive: true, diagnosedDate: '' };
    this.showCategoryPicker = false;
    this.showForm = true;
  }

  openEditForm(rf: RiskFactor): void {
    this.isEditing = true; this.error = null;
    this.selectedFactorDef = this.findFactorDef(rf.factorType);
    this.selectedCategory = this.findCategory(rf.factorType);
    this.clinicalDetails = { controlStatus: '', onMedication: false, medicationDetails: '', lastLabValue: '', monitoring: '' };
    this.formData = { ...rf, diagnosedDate: rf.diagnosedDate ? rf.diagnosedDate.split('T')[0] : '' };
    this.showForm = true;
  }

  closeForm(): void {
    this.showForm = false; this.showCategoryPicker = false; this.error = null;
    this.selectedFactorDef = null; this.selectedCategory = null;
  }

  findFactorDef(type: string): RiskFactorDefinition | null {
    for (const cat of RISK_FACTOR_CATEGORIES) {
      const f = cat.factors.find(f => f.type === type);
      if (f) return f;
    }
    return null;
  }

  findCategory(type: string): RiskFactorCategory | null {
    return RISK_FACTOR_CATEGORIES.find(c => c.factors.some(f => f.type === type)) || null;
  }

  buildNotes(): string {
    const parts: string[] = [];
    if (this.formData.notes?.trim()) parts.push(this.formData.notes.trim());
    if (this.clinicalDetails.controlStatus) parts.push(`Status: ${this.clinicalDetails.controlStatus}`);
    if (this.clinicalDetails.onMedication) parts.push(`On medication${this.clinicalDetails.medicationDetails ? ': ' + this.clinicalDetails.medicationDetails : ''}`);
    if (this.clinicalDetails.lastLabValue) parts.push(`Lab: ${this.clinicalDetails.lastLabValue}`);
    if (this.clinicalDetails.monitoring) parts.push(`Monitoring: ${this.clinicalDetails.monitoring}`);
    return parts.join(' | ');
  }

  submitForm(): void {
    if (!this.formData.factorType) { this.error = 'Please select a risk factor'; return; }
    this.error = null;
    const payload = { ...this.formData, medicalRecordId: this.medicalRecordId };
    payload.notes = this.buildNotes();
    if (payload.diagnosedDate?.trim()) payload.diagnosedDate = payload.diagnosedDate + 'T00:00:00';
    else delete payload.diagnosedDate;

    const req = this.isEditing && this.formData.id
      ? this.riskFactorService.updateRiskFactor(this.formData.id, payload)
      : this.riskFactorService.createRiskFactor(payload);
    req.subscribe({
      next: () => { this.load(); this.closeForm(); },
      error: (err) => { this.error = 'Failed to save: ' + (err.error?.error || 'Unknown error'); }
    });
  }

  deleteRiskFactor(id: number): void {
    if (!confirm('Remove this risk factor from the clinical profile?')) return;
    this.riskFactorService.deleteRiskFactor(id).subscribe({ next: () => this.load() });
  }

  toggleActive(rf: RiskFactor): void {
    this.riskFactorService.updateRiskFactor(rf.id!, { ...rf, isActive: !rf.isActive }).subscribe({ next: () => this.load() });
  }

  isFactorAlreadyAdded(factorType: string): boolean {
    return this.riskFactors.some(r => r.factorType === factorType);
  }

  getSevConfig(s: string): { icon: string; cls: string; color: string; label: string } {
    const m: Record<string, any> = {
      LOW:      { icon: '🟢', cls: 'sev-low',      color: '#10b981', label: 'Low Risk' },
      MEDIUM:   { icon: '🟡', cls: 'sev-medium',   color: '#f59e0b', label: 'Moderate' },
      HIGH:     { icon: '🟠', cls: 'sev-high',     color: '#f97316', label: 'High Risk' },
      CRITICAL: { icon: '🔴', cls: 'sev-critical', color: '#ef4444', label: 'Critical' }
    };
    return m[s] || m['MEDIUM'];
  }

  getCategoryIcon(type: string): string { return this.findCategory(type)?.icon || '⚠️'; }
  getCategoryColor(type: string): string { return this.findCategory(type)?.color || '#6b7280'; }

  getRiskPoints(rf: RiskFactor): number {
    if (!rf.isActive) return 0;
    const base: Record<string, number> = { LOW: 2, MEDIUM: 3, HIGH: 5, CRITICAL: 7 };
    let s = base[rf.severity] || 3;
    const t = rf.factorType?.toLowerCase() || '';
    if (t.includes('diabetes') || t.includes('hypertension') || t.includes('cardiovascular') || t.includes('stroke')) s *= 1.3;
    return Math.round(s * 10) / 10;
  }

  get totalRiskPoints(): number {
    return Math.min(25, Math.round(this.riskFactors.filter(f => f.isActive).reduce((s, f) => s + this.getRiskPoints(f), 0) * 10) / 10);
  }

  formatDate(d: string): string {
    if (!d) return 'N/A';
    return new Date(d).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
  }
}
