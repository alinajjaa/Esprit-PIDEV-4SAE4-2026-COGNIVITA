import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type Relationship =
  | 'FATHER' | 'MOTHER'
  | 'PATERNAL_GRANDFATHER' | 'PATERNAL_GRANDMOTHER'
  | 'MATERNAL_GRANDFATHER' | 'MATERNAL_GRANDMOTHER'
  | 'BROTHER' | 'SISTER' | 'SON' | 'DAUGHTER'
  | 'UNCLE' | 'AUNT' | 'COUSIN' | 'OTHER';

export type FamilyGender = 'MALE' | 'FEMALE' | 'OTHER';

export interface FamilyMember {
  id?: number;
  userId: number;
  fullName: string;
  relationship: Relationship;
  age?: number;
  isAlive?: boolean;
  hasAlzheimers?: boolean;
  hasDementia?: boolean;
  otherConditions?: string;
  parentMemberId?: number;
  gender?: FamilyGender;
  hereditaryRiskScore?: number;
  notes?: string;
  createdAt?: string;
}

export interface FamilyTreeNode extends FamilyMember {
  children?: FamilyTreeNode[];
}

export interface HereditaryRiskAnalysis {
  hereditaryRiskScore: number;
  riskLevel: string;
  riskPatterns: string[];
  totalFamilyMembers: number;
  affectedMembers: number;
  firstDegreeAffected: number;
  grandparentsAffected: number;
  affectedByRelationship: Record<string, number>;
  multiGenerational: boolean;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

@Injectable({ providedIn: 'root' })
export class FamilyTreeService {
  private base = '/api/family-tree';

  constructor(private http: HttpClient) {}

  getMembers(userId: number): Observable<ApiResponse<FamilyMember[]>> {
    return this.http.get<ApiResponse<FamilyMember[]>>(`${this.base}/user/${userId}`);
  }

  getTree(userId: number): Observable<ApiResponse<FamilyTreeNode[]>> {
    return this.http.get<ApiResponse<FamilyTreeNode[]>>(`${this.base}/user/${userId}/tree`);
  }

  getRiskAnalysis(userId: number): Observable<ApiResponse<HereditaryRiskAnalysis>> {
    return this.http.get<ApiResponse<HereditaryRiskAnalysis>>(`${this.base}/user/${userId}/risk-analysis`);
  }

  getStats(userId: number): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.base}/user/${userId}/stats`);
  }

  addMember(member: FamilyMember): Observable<ApiResponse<FamilyMember>> {
    return this.http.post<ApiResponse<FamilyMember>>(this.base, member);
  }

  updateMember(id: number, member: FamilyMember): Observable<ApiResponse<FamilyMember>> {
    return this.http.put<ApiResponse<FamilyMember>>(`${this.base}/${id}`, member);
  }

  deleteMember(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/${id}`);
  }
}
