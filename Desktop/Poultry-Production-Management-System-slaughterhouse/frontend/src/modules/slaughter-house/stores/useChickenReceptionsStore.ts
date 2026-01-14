// src/modules/slaughter-house/stores/useChickenReceptionsStore.ts
import { create } from 'zustand';
import { chickenReceptionsApi } from '../lib/slaughterhouseApi';

export interface ChickenReception {
  id?: number;
  delivery_notice_id?: number;
  slaughter_lot_id?: number;
  reception_date?: string;
  quantity_received?: number;
  average_weight?: number;
  total_weight?: number;
  vehicle_info?: string;
  driver_name?: string;
  temperature_on_arrival?: number;
  reception_notes?: string;
  quality_assessment?: string;
  received_by?: string;
  is_active?: boolean;
  created_at?: string;
  updated_at?: string;
}

interface ChickenReceptionsStore {
  // State
  receptions: ChickenReception[];
  currentReception: ChickenReception | null;
  isLoading: boolean;
  error: string | null;

  // Actions - CRUD
  fetchReceptions: () => Promise<void>;
  fetchReceptionById: (id: number) => Promise<void>;
  createReception: (reception: ChickenReception) => Promise<ChickenReception>;
  updateReception: (id: number, reception: ChickenReception) => Promise<ChickenReception>;
  deleteReception: (id: number) => Promise<void>;

  // Actions - Business Logic
  fetchActiveReceptions: () => Promise<void>;
  fetchByDateRange: (startDate: string, endDate: string) => Promise<void>;
  getSummary: () => Promise<any>;

  // Actions - Utility
  setCurrentReception: (reception: ChickenReception | null) => void;
  clearCurrentReception: () => void;
  setError: (error: string | null) => void;
  clearError: () => void;
}

export const useChickenReceptionsStore = create<ChickenReceptionsStore>((set, get) => ({
  // Initial State
  receptions: [],
  currentReception: null,
  isLoading: false,
  error: null,

  // Fetch all receptions
  fetchReceptions: async () => {
    set({ isLoading: true, error: null });
    try {
      const response = await chickenReceptionsApi.getAll();
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
      const response = await chickenReceptionsApi.getById(id);
      set({ currentReception: response.data, isLoading: false });
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to fetch reception',
        isLoading: false
      });
    }
  },

  // Create new reception
  createReception: async (reception: ChickenReception) => {
    set({ isLoading: true, error: null });
    try {
      const response = await chickenReceptionsApi.create(reception);
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

  // Update reception
  updateReception: async (id: number, reception: ChickenReception) => {
    set({ isLoading: true, error: null });
    try {
      const response = await chickenReceptionsApi.update(id, reception);
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

  // Delete reception
  deleteReception: async (id: number) => {
    set({ isLoading: true, error: null });
    try {
      await chickenReceptionsApi.delete(id);
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

  // Fetch active receptions
  fetchActiveReceptions: async () => {
    set({ isLoading: true, error: null });
    try {
      const response = await chickenReceptionsApi.getActive();
      set({ receptions: response.data, isLoading: false });
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to fetch active receptions',
        isLoading: false
      });
    }
  },

  // Fetch receptions by date range
  fetchByDateRange: async (startDate: string, endDate: string) => {
    set({ isLoading: true, error: null });
    try {
      const response = await chickenReceptionsApi.getByDateRange(startDate, endDate);
      set({ receptions: response.data, isLoading: false });
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to fetch receptions by date range',
        isLoading: false
      });
    }
  },

  // Get reception summary
  getSummary: async () => {
    set({ isLoading: true, error: null });
    try {
      const response = await chickenReceptionsApi.getSummary();
      set({ isLoading: false });
      return response.data;
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to get reception summary',
        isLoading: false
      });
      throw error;
    }
  },

  // Set current reception
  setCurrentReception: (reception: ChickenReception | null) => {
    set({ currentReception: reception });
  },

  // Clear current reception
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
