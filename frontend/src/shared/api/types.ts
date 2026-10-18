export type ScopeType = 'GLOBAL' | 'BREEDER_SITE' | 'HATCHERY' | 'FARM' | 'SLAUGHTERHOUSE';
export const SCOPE_TYPES: ScopeType[] = ['GLOBAL', 'BREEDER_SITE', 'HATCHERY', 'FARM', 'SLAUGHTERHOUSE'];

export interface SpringPage<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number }
export interface PageResponse<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number }

// identity
export type UserStatus = 'PENDING_ACTIVATION' | 'ACTIVE' | 'LOCKED' | 'DISABLED';
export interface User {
  id: string; email: string; username?: string; displayName?: string; phone?: string; locale?: string;
  status: UserStatus; employeeId?: string; lastLoginAt?: string; createdAt: string;
}
export interface CreateUserRequest {
  email: string; username?: string; displayName?: string; locale?: string; sendActivationEmail: boolean; initialPassword?: string;
}

// admin / access
export interface Role {
  id: string; code: string; nameRo: string; nameEn?: string; description?: string; module?: string;
  allowedScopeTypes: ScopeType[]; systemDefined: boolean; active: boolean; permissions: string[]; activeAssignments: number;
}
export interface RoleRequest {
  code: string; nameRo: string; nameEn?: string; description?: string; module?: string;
  allowedScopeTypes: ScopeType[]; permissions: string[]; active?: boolean;
}
export interface Permission { id: string; key: string; module?: string; description?: string; systemDefined: boolean }
export interface PermissionRequest { key: string; module?: string; description?: string }
export interface Assignment {
  id: string; userId: string; roleCode: string; roleName?: string; scopeType: ScopeType; scopeId?: string; scopeName?: string;
  assignedBy?: string; assignedAt: string; validFrom?: string; validTo?: string; revokedAt?: string; reason?: string; active: boolean;
}
export interface AssignmentRequest { userId: string; roleCode: string; scopeType: ScopeType; scopeId?: string; reason?: string }
export interface Site { siteId: string; siteType: string; code: string; name?: string; county?: string; active: boolean }

// farm
export interface Farm { id: string; code: string; name: string; address?: string; county?: string; active: boolean; createdAt: string; updatedAt: string }
export interface FarmRequest { code: string; name: string; address?: string; county?: string; active?: boolean }
export interface House { id: string; farmId: string; code: string; name: string; capacity: number; areaM2: number; active: boolean }
export interface HouseRequest { code: string; name: string; capacity: number; areaM2: number; active?: boolean }
export interface Silo { id: string; farmId: string; code: string; name: string; capacityKg: number }
export interface SiloRequest { code: string; name: string; capacityKg: number }
export type LotStatus = 'ACTIVE' | 'CLOSED' | string;
export interface Lot {
  id: string; farmId: string; houseId: string; breedCode: string; chickBatchId?: string; arrivalDate: string;
  femaleCount: number; maleCount: number; status: LotStatus; expectedCatchDate?: string;
}
export interface LotRequest { houseId: string; breedCode: string; arrivalDate: string; femaleCount: number; maleCount: number; expectedCatchDate?: string }
export interface DailySheet {
  id: string; lotId: string; date: string; mortality: number; feedKg?: number; waterLiters?: number; avgWeightG?: number;
  temperatureMin?: number; temperatureMax?: number; humidityPct?: number; comments?: string;
}
export interface DailySheetRequest {
  date: string; mortality: number; feedKg?: number; waterLiters?: number; avgWeightG?: number;
  temperatureMin?: number; temperatureMax?: number; humidityPct?: number; comments?: string;
}
export interface FarmDelivery { id: string; lotId: string; dispatchDate: string; quantity: number; avgWeightKg: number; fciRef?: string }
export interface FarmDeliveryRequest { dispatchDate: string; quantity: number; avgWeightKg: number; fciRef?: string }

// catalog
export interface Breed { id: string; code: string; name: string; kind: string; active: boolean }

// other site modules (list/create only for now)
export interface SiteSummary { id: string; code: string; name?: string; address?: string; county?: string; active?: boolean }
