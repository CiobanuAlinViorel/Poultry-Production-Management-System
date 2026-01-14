// src/stores/usePoultryHouseStore.ts
import { create } from 'zustand';
import broilerFarmApi from '@/modules/broiler-farm/lib/axios';

export type PoultryHouseType = 'OPEN' | 'CLOSED' | 'SEMI_CLOSED';
export type PoultryHouseStatus = 'EMPTY' | 'OCCUPIED' | 'MAINTENANCE';

export interface PoultryHouse {
    id?: number;
    farmId: number;
    capacity: number;
    area: number;
    type: PoultryHouseType;
    equipmentType?: string;
    status: PoultryHouseStatus;
    currentOccupancy: number;
    currentLot?: string;
}

interface PoultryHouseStore {
    houses: PoultryHouse[];
    isLoading: boolean;
    error: string | null;

    fetchHouses: (farmId?: number) => Promise<void>;
    getHouseById: (id: number) => Promise<PoultryHouse>;
    createHouse: (house: PoultryHouse) => Promise<PoultryHouse>;
    updateHouse: (id: number, house: PoultryHouse) => Promise<PoultryHouse>;
    deleteHouse: (id: number) => Promise<void>;
    getAvailableHouses: (farmId: number) => PoultryHouse[];
    clearError: () => void;
}

export const usePoultryHouseStore = create<PoultryHouseStore>((set, get) => ({
    houses: [],
    isLoading: false,
    error: null,

    fetchHouses: async (farmId?: number) => {
        set({ isLoading: true, error: null });
        console.log(farmId)
        try {
            const url = farmId
                ? `/api/poultry-houses/farm/${farmId}`
                : '/api/poultry-houses';
            const response = await broilerFarmApi.get(url);
            set({ houses: response.data, isLoading: false });
        } catch (error: any) {
            set({
                error: error.response?.data?.message || 'Failed to fetch poultry houses',
                isLoading: false
            });
        }
    },

    getHouseById: async (id: number) => {
        set({ isLoading: true, error: null });
        try {
            const response = await broilerFarmApi.get(`/api/poultry-houses/${id}`);
            set({ isLoading: false });
            return response.data;
        } catch (error: any) {
            set({
                error: error.response?.data?.message || 'Failed to fetch house',
                isLoading: false
            });
            throw error;
        }
    },

    createHouse: async (house: PoultryHouse) => {
        set({ isLoading: true, error: null });
        try {
            const response = await broilerFarmApi.post('/api/poultry-houses', house);
            set(state => ({
                houses: [...state.houses, response.data],
                isLoading: false
            }));
            return response.data;
        } catch (error: any) {
            set({
                error: error.response?.data?.message || 'Failed to create house',
                isLoading: false
            });
            throw error;
        }
    },

    updateHouse: async (id: number, house: PoultryHouse) => {
        set({ isLoading: true, error: null });
        try {
            const response = await broilerFarmApi.put(`/api/poultry-houses/${id}`, house);
            set(state => ({
                houses: state.houses.map(h => h.id === id ? response.data : h),
                isLoading: false
            }));
            return response.data;
        } catch (error: any) {
            set({
                error: error.response?.data?.message || 'Failed to update house',
                isLoading: false
            });
            throw error;
        }
    },

    deleteHouse: async (id: number) => {
        set({ isLoading: true, error: null });
        try {
            await broilerFarmApi.delete(`/api/poultry-houses/${id}`);
            set(state => ({
                houses: state.houses.filter(h => h.id !== id),
                isLoading: false
            }));
        } catch (error: any) {
            set({
                error: error.response?.data?.message || 'Failed to delete house',
                isLoading: false
            });
            throw error;
        }
    },

    getAvailableHouses: (farmId: number) => {
        return get().houses.filter(h => h.farmId === farmId && h.status === 'EMPTY');
    },

    clearError: () => set({ error: null }),
}));