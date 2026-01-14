// src/stores/useReceptionStore.ts
import { create } from 'zustand';
import broilerFarmApi from '@/modules/broiler-farm/lib/axios';

export interface ReceptionLine {
  id?: number;
  poultryHouseId: number;
  poultryHouseName?: string;
  chicksAlive: number;
  chicksDOA: number;
  chicksWeak: number;
  qualityGrade: 'A' | 'B' | 'C' | 'D' | 'F';  // ← Enum string literal
  notes?: string;
  breed?: string;
  hatcherySource?: string;
}

export interface Reception {
  id?: number;
  farmId: number;
  employeeid: number;
  receptionDate: string;
  transportConditions?: string;
  truckInfo?: string;
  referenceDocument?: string;
  receptionStatus?: 'DRAFT' | 'CONFIRMED';
  lines: ReceptionLine[];
}

interface ReceptionStore {
  // State
  receptions: Reception[];
  currentReception: Reception | null;
  isLoading: boolean;
  error: string | null;

  // Actions - CRUD
  fetchReceptions: () => Promise<void>;
  fetchReceptionById: (id: number) => Promise<void>;
  createReception: (reception: Reception) => Promise<Reception>;
  updateReception: (id: number, reception: Reception) => Promise<Reception>;
  finalizeReception: (id: number) => Promise<Reception>;
  deleteReception: (id: number) => Promise<void>;

  // Actions - Draft Management
  setCurrentReception: (reception: Reception | null) => void;
  addLine: (line: ReceptionLine) => void;
  updateLine: (index: number, line: ReceptionLine) => void;
  removeLine: (index: number) => void;
  clearCurrentReception: () => void;

  // Actions - Error Handling
  setError: (error: string | null) => void;
  clearError: () => void;
}

export const useReceptionStore = create<ReceptionStore>((set, get) => ({
  // Initial State
  receptions: [],
  currentReception: null,
  isLoading: false,
  error: null,

  // Fetch all receptions
  fetchReceptions: async () => {
    set({ isLoading: true, error: null });
    try {
      const response = await broilerFarmApi.get('/api/chicks-receptions');
      set({ receptions: response.data, isLoading: false });
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to fetch receptions',
        isLoading: false
      });
    }
  },

  // Fetch single reception
  fetchReceptionById: async (id: number) => {
    set({ isLoading: true, error: null });
    try {
      const response = await broilerFarmApi.get(`/api/chicks-receptions/${id}`);
      set({ currentReception: response.data, isLoading: false });
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to fetch reception',
        isLoading: false
      });
    }
  },

  // Create new reception (DRAFT)
  createReception: async (reception: Reception) => {
    set({ isLoading: true, error: null });
    try {
      const response = await broilerFarmApi.post('/api/chicks-receptions', reception);
      set(state => ({
        receptions: [...state.receptions, response.data],
        currentReception: response.data,
        isLoading: false
      }));
      return response.data;
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to create reception',
        isLoading: false
      });
      throw error;
    }
  },

  // Update reception (only DRAFT)
  updateReception: async (id: number, reception: Reception) => {
    set({ isLoading: true, error: null });
    try {
      const response = await broilerFarmApi.put(`/api/chicks-receptions/${id}`, reception);
      set(state => ({
        receptions: state.receptions.map(r => r.id === id ? response.data : r),
        currentReception: response.data,
        isLoading: false
      }));
      return response.data;
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to update reception',
        isLoading: false
      });
      throw error;
    }
  },

  // Finalize reception
  finalizeReception: async (id: number) => {
    set({ isLoading: true, error: null });
    try {
      const response = await broilerFarmApi.post(`/api/chicks-receptions/${id}/finalize`);
      set(state => ({
        receptions: state.receptions.map(r => r.id === id ? response.data : r),
        currentReception: response.data,
        isLoading: false
      }));
      return response.data;
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to finalize reception',
        isLoading: false
      });
      throw error;
    }
  },

  // Delete reception (only DRAFT)
  deleteReception: async (id: number) => {
    set({ isLoading: true, error: null });
    try {
      await broilerFarmApi.delete(`/api/chicks-receptions/${id}`);
      set(state => ({
        receptions: state.receptions.filter(r => r.id !== id),
        isLoading: false
      }));
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to delete reception',
        isLoading: false
      });
      throw error;
    }
  },

  // Draft management - set current reception
  setCurrentReception: (reception: Reception | null) => {
    set({ currentReception: reception });
  },

  // Draft management - add line
  addLine: (line: ReceptionLine) => {
    set(state => ({
      currentReception: state.currentReception
        ? {
          ...state.currentReception,
          lines: [...state.currentReception.lines, line]
        }
        : null
    }));
  },

  // Draft management - update line
  updateLine: (index: number, line: ReceptionLine) => {
    set(state => ({
      currentReception: state.currentReception
        ? {
          ...state.currentReception,
          lines: state.currentReception.lines.map((l, i) => i === index ? line : l)
        }
        : null
    }));
  },

  // Draft management - remove line
  removeLine: (index: number) => {
    set(state => ({
      currentReception: state.currentReception
        ? {
          ...state.currentReception,
          lines: state.currentReception.lines.filter((_, i) => i !== index)
        }
        : null
    }));
  },

  // Draft management - clear current reception
  clearCurrentReception: () => {
    set({ currentReception: null });
  },

  // Error handling
  setError: (error: string | null) => {
    set({ error });
  },

  clearError: () => {
    set({ error: null });
  },
}));