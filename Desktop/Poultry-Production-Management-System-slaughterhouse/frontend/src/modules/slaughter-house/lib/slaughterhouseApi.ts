// src/modules/slaughter-house/lib/slaughterhouseApi.ts
import axios from 'axios';

// Create axios instance with base configuration
const slaughterhouseApi = axios.create({
    baseURL: 'http://localhost:8081/api',
    headers: {
        'Content-Type': 'application/json',
    },
});

// Request interceptor - add JWT token from localStorage
slaughterhouseApi.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Response interceptor - handle 401 errors (token expired)
slaughterhouseApi.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            // Token expired or invalid
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

// ============================================
// PRODUCTS API
// ============================================
export const productsApi = {
    // GET all products
    getAll: () => slaughterhouseApi.get('/products'),
    
    // GET product by ID
    getById: (id: number) => slaughterhouseApi.get(`/products/${id}`),
    
    // POST create product
    create: (data: any) => slaughterhouseApi.post('/products', data),
    
    // PUT update product
    update: (id: number, data: any) => slaughterhouseApi.put(`/products/${id}`, data),
    
    // DELETE product (soft delete)
    delete: (id: number) => slaughterhouseApi.delete(`/products/${id}`),
    
    // PATCH inspect product
    inspect: (id: number, passed: boolean) => 
        slaughterhouseApi.patch(`/products/${id}/inspect?passed=${passed}`),
    
    // PATCH package product
    packageProduct: (id: number) => 
        slaughterhouseApi.patch(`/products/${id}/package`),
    
    // GET product types (enum values)
    getTypes: () => slaughterhouseApi.get('/products/types'),
    
    // GET products by status
    getByStatus: (status: string) => 
        slaughterhouseApi.get(`/products/status/${status}`),
    
    // GET active products
    getActive: () => slaughterhouseApi.get('/products/active'),
    
    // GET products by slaughter lot
    getBySlaughterLot: (lotId: number) => 
        slaughterhouseApi.get(`/products/slaughter-lot/${lotId}`),
    
    // GET products by date range
    getByDateRange: (startDate: string, endDate: string) => 
        slaughterhouseApi.get(`/products/date-range?startDate=${startDate}&endDate=${endDate}`),
};

// ============================================
// SLAUGHTER LOTS API
// ============================================
export const slaughterLotsApi = {
    // GET all slaughter lots
    getAll: () => slaughterhouseApi.get('/slaughter-lots'),
    
    // GET slaughter lot by ID
    getById: (id: number) => slaughterhouseApi.get(`/slaughter-lots/${id}`),
    
    // GET slaughter lot by lot number
    getByLotNumber: (lotNumber: string) => 
        slaughterhouseApi.get(`/slaughter-lots/lot-number/${lotNumber}`),
    
    // POST create slaughter lot
    create: (data: any) => slaughterhouseApi.post('/slaughter-lots', data),
    
    // PUT update slaughter lot
    update: (id: number, data: any) => 
        slaughterhouseApi.put(`/slaughter-lots/${id}`, data),
    
    // DELETE slaughter lot (soft delete)
    delete: (id: number) => slaughterhouseApi.delete(`/slaughter-lots/${id}`),
    
    // GET slaughter lots by status
    getByStatus: (status: string) => 
        slaughterhouseApi.get(`/slaughter-lots/status/${status}`),
    
    // GET active slaughter lots
    getActive: () => slaughterhouseApi.get('/slaughter-lots/active'),
    
    // PATCH update quantity
    updateQuantity: (id: number, quantity: number) => 
        slaughterhouseApi.patch(`/slaughter-lots/${id}/quantity?quantity=${quantity}`),
    
    // POST calculate total weight
    calculateWeight: (id: number) => 
        slaughterhouseApi.post(`/slaughter-lots/${id}/calculate-weight`),
    
    // GET mortality statistics
    getMortalityStats: (id: number) => 
        slaughterhouseApi.get(`/slaughter-lots/${id}/mortality-stats`),
};

// ============================================
// CHICKEN RECEPTIONS API
// ============================================
export const chickenReceptionsApi = {
    // GET all chicken receptions
    getAll: () => slaughterhouseApi.get('/chicken-receptions'),
    
    // GET chicken reception by ID
    getById: (id: number) => slaughterhouseApi.get(`/chicken-receptions/${id}`),
    
    // POST create chicken reception
    create: (data: any) => slaughterhouseApi.post('/chicken-receptions', data),
    
    // PUT update chicken reception
    update: (id: number, data: any) => 
        slaughterhouseApi.put(`/chicken-receptions/${id}`, data),
    
    // DELETE chicken reception (soft delete)
    delete: (id: number) => slaughterhouseApi.delete(`/chicken-receptions/${id}`),
    
    // GET active chicken receptions
    getActive: () => slaughterhouseApi.get('/chicken-receptions/active'),
    
    // GET chicken receptions by date range
    getByDateRange: (startDate: string, endDate: string) => 
        slaughterhouseApi.get(`/chicken-receptions/date-range?startDate=${startDate}&endDate=${endDate}`),
    
    // GET reception summary
    getSummary: () => slaughterhouseApi.get('/chicken-receptions/summary'),
};

export default slaughterhouseApi;
