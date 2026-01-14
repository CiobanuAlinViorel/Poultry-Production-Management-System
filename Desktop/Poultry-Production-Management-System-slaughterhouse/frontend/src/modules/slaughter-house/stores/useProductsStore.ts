// src/modules/slaughter-house/stores/useProductsStore.ts
import { create } from 'zustand';
import { productsApi } from '../lib/slaughterhouseApi';

export interface Product {
  id?: number;
  product_type: string;
  slaughter_lot_id: number;
  cut?: string;
  weight_value?: number;
  weight_unit?: string;
  quality_grade?: string;
  quality_score?: number;
  production_date?: string;
  status: string;
  batch_number?: string;
  packaging_date?: string;
  expiry_date?: string;
  storage_location?: string;
  inspection_passed?: boolean;
  inspection_date?: string;
  inspector_notes?: string;
  is_active: boolean;
  created_at?: string;
  updated_at?: string;
}

interface ProductsStore {
  // State
  products: Product[];
  currentProduct: Product | null;
  productTypes: any[];
  isLoading: boolean;
  error: string | null;

  // Actions - CRUD
  fetchProducts: () => Promise<void>;
  fetchProductById: (id: number) => Promise<void>;
  createProduct: (product: Product) => Promise<Product>;
  updateProduct: (id: number, product: Product) => Promise<Product>;
  deleteProduct: (id: number) => Promise<void>;

  // Actions - Business Logic
  inspectProduct: (id: number, passed: boolean) => Promise<void>;
  packageProduct: (id: number) => Promise<void>;
  fetchProductTypes: () => Promise<void>;
  fetchActiveProducts: () => Promise<void>;
  fetchByStatus: (status: string) => Promise<void>;

  // Actions - Utility
  setCurrentProduct: (product: Product | null) => void;
  clearCurrentProduct: () => void;
  setError: (error: string | null) => void;
  clearError: () => void;
}

export const useProductsStore = create<ProductsStore>((set, get) => ({
  // Initial State
  products: [],
  currentProduct: null,
  productTypes: [],
  isLoading: false,
  error: null,

  // Fetch all products
  fetchProducts: async () => {
    set({ isLoading: true, error: null });
    try {
      const response = await productsApi.getAll();
      set({ products: response.data, isLoading: false });
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to fetch products',
        isLoading: false
      });
    }
  },

  // Fetch single product
  fetchProductById: async (id: number) => {
    set({ isLoading: true, error: null });
    try {
      const response = await productsApi.getById(id);
      set({ currentProduct: response.data, isLoading: false });
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to fetch product',
        isLoading: false
      });
    }
  },

  // Create new product
  createProduct: async (product: Product) => {
    set({ isLoading: true, error: null });
    try {
      const response = await productsApi.create(product);
      set(state => ({
        products: [...state.products, response.data],
        currentProduct: response.data,
        isLoading: false
      }));
      return response.data;
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to create product',
        isLoading: false
      });
      throw error;
    }
  },

  // Update product
  updateProduct: async (id: number, product: Product) => {
    set({ isLoading: true, error: null });
    try {
      const response = await productsApi.update(id, product);
      set(state => ({
        products: state.products.map(p => p.id === id ? response.data : p),
        currentProduct: response.data,
        isLoading: false
      }));
      return response.data;
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to update product',
        isLoading: false
      });
      throw error;
    }
  },

  // Delete product
  deleteProduct: async (id: number) => {
    set({ isLoading: true, error: null });
    try {
      await productsApi.delete(id);
      set(state => ({
        products: state.products.filter(p => p.id !== id),
        isLoading: false
      }));
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to delete product',
        isLoading: false
      });
      throw error;
    }
  },

  // Inspect product
  inspectProduct: async (id: number, passed: boolean) => {
    set({ isLoading: true, error: null });
    try {
      await productsApi.inspect(id, passed);
      // Refresh product data
      const response = await productsApi.getAll();
      set({ products: response.data, isLoading: false });
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to inspect product',
        isLoading: false
      });
      throw error;
    }
  },

  // Package product
  packageProduct: async (id: number) => {
    set({ isLoading: true, error: null });
    try {
      await productsApi.packageProduct(id);
      // Refresh product data
      const response = await productsApi.getAll();
      set({ products: response.data, isLoading: false });
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to package product',
        isLoading: false
      });
      throw error;
    }
  },

  // Fetch product types (enum)
  fetchProductTypes: async () => {
    set({ isLoading: true, error: null });
    try {
      const response = await productsApi.getTypes();
      set({ productTypes: response.data, isLoading: false });
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to fetch product types',
        isLoading: false
      });
    }
  },

  // Fetch active products
  fetchActiveProducts: async () => {
    set({ isLoading: true, error: null });
    try {
      const response = await productsApi.getActive();
      set({ products: response.data, isLoading: false });
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to fetch active products',
        isLoading: false
      });
    }
  },

  // Fetch products by status
  fetchByStatus: async (status: string) => {
    set({ isLoading: true, error: null });
    try {
      const response = await productsApi.getByStatus(status);
      set({ products: response.data, isLoading: false });
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to fetch products by status',
        isLoading: false
      });
    }
  },

  // Set current product
  setCurrentProduct: (product: Product | null) => {
    set({ currentProduct: product });
  },

  // Clear current product
  clearCurrentProduct: () => {
    set({ currentProduct: null });
  },

  // Error handling
  setError: (error: string | null) => {
    set({ error });
  },

  clearError: () => {
    set({ error: null });
  },
}));
