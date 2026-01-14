// src/services/employeeService.ts
// Exemplu de service pentru API calls cu axios

import broilerFarmApi from '@/modules/broiler-farm/lib/axios';

export interface Employee {
  id: number;
  username: string;
  email: string;
  isActive: boolean;
  roles: string[];
  employee: {
    firstName: string;
    lastName: string;
    phone: string;
    role: string;
  };
}

export interface CreateEmployeeRequest {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  username: string;
  role: 'WORKER' | 'FARM_MANAGER' | 'VETERINARIAN';
  hireDate: string;
  farmId: number;
}

// GET - Toți angajații
export const getAllEmployees = async (): Promise<Employee[]> => {
  const response = await broilerFarmApi.get('/api/employees');
  return response.data;
};

// GET - Angajat după ID
export const getEmployeeById = async (id: number): Promise<Employee> => {
  const response = await broilerFarmApi.get(`/api/employees/${id}`);
  return response.data;
};

// POST - Creare angajat
export const createEmployee = async (data: CreateEmployeeRequest): Promise<Employee> => {
  const response = await broilerFarmApi.post('/api/employees', data);
  return response.data;
};

// PUT - Actualizare angajat
export const updateEmployee = async (id: number, data: Partial<CreateEmployeeRequest>): Promise<Employee> => {
  const response = await broilerFarmApi.put(`/api/employees/${id}`, data);
  return response.data;
};

// DELETE - Ștergere angajat
export const deleteEmployee = async (id: number): Promise<void> => {
  await broilerFarmApi.delete(`/api/employees/${id}`);
};

// POST - Reset parolă
export const resetPassword = async (id: number): Promise<{ message: string; newPassword: string }> => {
  const response = await broilerFarmApi.post(`/api/employees/${id}/reset-password`);
  return response.data;
};

// Exemplu de utilizare în componentă:
/*
import { getAllEmployees, createEmployee } from '@/services/employeeService';

function EmployeesPage() {
  const [employees, setEmployees] = useState([]);

  useEffect(() => {
    loadEmployees();
  }, []);

  const loadEmployees = async () => {
    try {
      const data = await getAllEmployees();
      setEmployees(data);
    } catch (error) {
      console.error('Error loading employees:', error);
    }
  };

  const handleCreate = async (formData) => {
    try {
      await createEmployee(formData);
      await loadEmployees(); // Reload lista
    } catch (error) {
      console.error('Error creating employee:', error);
    }
  };

  return (
    <div>
      {employees.map(emp => (
        <div key={emp.id}>{emp.username}</div>
      ))}
    </div>
  );
}
*/