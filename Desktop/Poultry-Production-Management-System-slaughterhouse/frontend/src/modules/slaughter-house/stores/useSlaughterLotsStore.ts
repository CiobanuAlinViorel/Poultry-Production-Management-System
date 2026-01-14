// src/modules/slaughter-house/stores/useSlaughterLotsStore.ts
import { create } from 'zustand';
import { slaughterLotsApi } from '../lib/slaughterhouseApi';

export interface SlaughterLot {
  id?: number;
  lot_number?: string;
  arrival_date?: string;
  breed?: string;
  quantity?: number;
  average_weight?: number;
  total_weight?: number;
  source_farm?: string;
  health_status?: string;
  status?: string;
  temperature?: number;
  humidity?: number;
  mortality_count?: number;
  quality_score?: number;
  notes?: string;
  is_active?: boolean;
  created_at?: string;
  updated_at?: string;
}

interface SlaughterLotsStore {
  // State
  slaughterLots: SlaughterLot[];
  currentLot: SlaughterLot | null;
  isLoading: boolean;
  error: string | null;

  // Actions - CRUD
  fetchSlaughterLots: () => Promise<void>;
  fetchSlaughterLotById: (id: number) => Promise<void>;
  fetchByLotNumber: (lotNumber: string) => Promise<void>;
  createSlaughterLot: (lot: SlaughterLot) => Promise<SlaughterLot>;
  updateSlaughterLot: (id: number, lot: SlaughterLot) => Promise<SlaughterLot>;
  deleteSlaughterLot: (id: number) => Promise<void>;

  // Actions - Business Logic
  fetchActiveSlaughterLots: () => Promise<void>;
  fetchByStatus: (status: string) => Promise<void>;
  updateQuantity: (id: number, quantity: number) => Promise<void>;
  calculateWeight: (id: number) => Promise<void>;
  getMortalityStats: (id: number) => Promise<any>;

  // Actions - Utility
  setCurrentLot: (lot: SlaughterLot | null) => void;
  clearCurrentLot: () => void;
  setError: (error: string | null) => void;
  clearError: () => void;
}

export const useSlaughterLotsStore = create<SlaughterLotsStore>((set, get) => ({
  // Initial State
  slaughterLots: [],
  currentLot: null,
  isLoading: false,
  error: null,

  // Fetch all slaughter lots
  fetchSlaughterLots: async () => {
    set({ isLoading: true, error: null });
    try {
      const response = await slaughterLotsApi.getAll();
      set({ slaughterLots: response.data, isLoading: false });
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to fetch slaughter lots',
        isLoading: false
      });
    }
  },

  // Fetch single slaughter lot by ID
  fetchSlaughterLotById: async (id: number) => {
    set({ isLoading: true, error: null });
    try {
      const response = await slaughterLotsApi.getById(id);
      set({ currentLot: response.data, isLoading: false });
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to fetch slaughter lot',
        isLoading: false
      });
    }
  },

  // Fetch slaughter lot by lot number
  fetchByLotNumber: async (lotNumber: string) => {
    set({ isLoading: true, error: null });
    try {
      const response = await slaughterLotsApi.getByLotNumber(lotNumber);
      set({ currentLot: response.data, isLoading: false });
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to fetch slaughter lot',
        isLoading: false
      });
    }
  },

  // Create new slaughter lot
  createSlaughterLot: async (lot: SlaughterLot) => {
    set({ isLoading: true, error: null });
    try {
      const response = await slaughterLotsApi.create(lot);
      set(state => ({
        slaughterLots: [...state.slaughterLots, response.data],
        currentLot: response.data,
        isLoading: false
      }));
      return response.data;
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to create slaughter lot',
        isLoading: false
      });
      throw error;
    }
  },

  // Update slaughter lot
  updateSlaughterLot: async (id: number, lot: SlaughterLot) => {
    set({ isLoading: true, error: null });
    try {
      const response = await slaughterLotsApi.update(id, lot);
      set(state => ({
        slaughterLots: state.slaughterLots.map(l => l.id === id ? response.data : l),
        currentLot: response.data,
        isLoading: false
      }));
      return response.data;
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to update slaughter lot',
        isLoading: false
      });
      throw error;
    }
  },

  // Delete slaughter lot
  deleteSlaughterLot: async (id: number) => {
    set({ isLoading: true, error: null });
    try {
      await slaughterLotsApi.delete(id);
      set(state => ({
        slaughterLots: state.slaughterLots.filter(l => l.id !== id),
        isLoading: false
      }));
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to delete slaughter lot',
        isLoading: false
      });
      throw error;
    }
  },

  // Fetch active slaughter lots
  fetchActiveSlaughterLots: async () => {
    set({ isLoading: true, error: null });
    try {
      const response = await slaughterLotsApi.getActive();
      set({ slaughterLots: response.data, isLoading: false });
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to fetch active slaughter lots',
        isLoading: false
      });
    }
  },

  // Fetch slaughter lots by status
  fetchByStatus: async (status: string) => {
    set({ isLoading: true, error: null });
    try {
      const response = await slaughterLotsApi.getByStatus(status);
      set({ slaughterLots: response.data, isLoading: false });
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to fetch slaughter lots by status',
        isLoading: false
      });
    }
  },

  // Update quantity
  updateQuantity: async (id: number, quantity: number) => {
    set({ isLoading: true, error: null });
    try {
      await slaughterLotsApi.updateQuantity(id, quantity);
      // Refresh data
      const response = await slaughterLotsApi.getAll();
      set({ slaughterLots: response.data, isLoading: false });
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to update quantity',
        isLoading: false
      });
      throw error;
    }
  },

  // Calculate total weight
  calculateWeight: async (id: number) => {
    set({ isLoading: true, error: null });
    try {
      await slaughterLotsApi.calculateWeight(id);
      // Refresh data
      const response = await slaughterLotsApi.getAll();
      set({ slaughterLots: response.data, isLoading: false });
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to calculate weight',
        isLoading: false
      });
      throw error;
    }
  },

  // Get mortality statistics
  getMortalityStats: async (id: number) => {
    set({ isLoading: true, error: null });
    try {
      const response = await slaughterLotsApi.getMortalityStats(id);
      set({ isLoading: false });
      return response.data;
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to get mortality stats',
        isLoading: false
      });
      throw error;
    }
  },

  // Set current lot
  setCurrentLot: (lot: SlaughterLot | null) => {
    set({ currentLot: lot });
  },

  // Clear current lot
  clearCurrentLot: () => {
    set({ currentLot: null });
  },

  // Error handling
  setError: (error: string | null) => {
    set({ error });
  },

  clearError: () => {
    set({ error: null });
  },
}));
