// src/lib/axios.ts
import axios from 'axios';

// Creează instanța axios cu baseURL
const broilerFarmApi = axios.create({
    baseURL: 'http://localhost:8082/broiler-farm', // Actualizează cu URL-ul tău
    headers: {
        'Content-Type': 'application/json',
    },
});

// Request interceptor - adaugă token-ul JWT la fiecare request
broilerFarmApi.interceptors.request.use(
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

// Response interceptor - handlează erorile 401 (Unauthorized)
broilerFarmApi.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            // Token expirat sau invalid
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

export default broilerFarmApi;