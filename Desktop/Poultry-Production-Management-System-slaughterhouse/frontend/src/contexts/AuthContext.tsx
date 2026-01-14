// src/contexts/AuthContext.tsx
import broilerFarmApi from '@/modules/broiler-farm/lib/axios';
import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';


export type UserModule = 'broiler-farm' | 'slaughter-house';

interface User {
    id: number;
    email: string;
    username: string;
    roles: string[];
    employeeId?: number;
    farmId?: number;
}

interface AuthContextType {
    user: User | null;
    token: string | null;
    currentModule: UserModule | null;
    login: (email: string, password: string, module: UserModule) => Promise<void>;
    logout: () => void;
    isAuthenticated: boolean;
    isLoading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

// 🔄 Module-specific API base URLs
const MODULE_API_URLS: Record<UserModule, string> = {
    'broiler-farm': 'http://localhost:8082/broiler-farm',
    'slaughter-house': 'http://localhost:8082/broiler-farm',
};

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<User | null>(null);
    const [token, setToken] = useState<string | null>(null);
    const [currentModule, setCurrentModule] = useState<UserModule | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    // La montare, verifică dacă există token salvat
    useEffect(() => {
        const savedToken = localStorage.getItem('token');
        const savedUser = localStorage.getItem('user');
        const savedModule = localStorage.getItem('module') as UserModule | null;

        if (savedToken && savedUser && savedModule) {
            setToken(savedToken);
            setUser(JSON.parse(savedUser));
            setCurrentModule(savedModule);

            // ✨ Update axios baseURL based on saved module
            updateApiBaseUrl(savedModule);
        }
        setIsLoading(false);
    }, []);

    // ✨ Helper function to update API base URL
    const updateApiBaseUrl = (module: UserModule) => {
        broilerFarmApi.defaults.baseURL = MODULE_API_URLS[module];
    };

    const login = async (email: string, password: string, module: UserModule) => {
        try {
            // ✨ Set the correct API URL before login
            updateApiBaseUrl(module);

            const response = await broilerFarmApi.post('/api/auth/login', { email, password });
            const { token, email: userEmail, username, roles, employeeId, farmId } = response.data;

            // Save to localStorage
            localStorage.setItem('token', token);
            localStorage.setItem('module', module);

            const userData = { email: userEmail, username, roles, employeeId, farmId };
            localStorage.setItem('user', JSON.stringify(userData));

            // Update state
            setToken(token);
            setUser({ id: 0, ...userData });
            setCurrentModule(module);

        } catch (error: any) {
            throw new Error(error.response?.data?.error || 'Login failed');
        }
    };

    const logout = async () => {
        try {
            if (token && currentModule) {
                // ✨ Make sure we use the correct API
                updateApiBaseUrl(currentModule);
                await broilerFarmApi.post('/api/auth/logout');
            }
        } catch (error) {
            console.error('Logout error:', error);
        } finally {
            // Clear everything
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            localStorage.removeItem('module');
            setToken(null);
            setUser(null);
            setCurrentModule(null);
        }
    };

    return (
        <AuthContext.Provider
            value={{
                user,
                token,
                currentModule,
                login,
                logout,
                isAuthenticated: !!token,
                isLoading,
            }}
        >
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
}