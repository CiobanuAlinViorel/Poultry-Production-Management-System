// src/App.tsx
import { Routes, Route, Navigate } from "react-router";
import { ThemeProvider } from "@/components/general/ThemeProvider";
import { AuthProvider } from "@/contexts/AuthContext";

import { Layout } from "@/components/Layout";
import HomePage from "./modules/broiler-farm/pages/HomePage";
import ChicksReceptionsPage from "./modules/broiler-farm/pages/ChicksReceptionsPage";
import CreateReceptionPage from "./modules/broiler-farm/pages/CreateReceptionPage";
import PoultryHousesPage from "./modules/broiler-farm/pages/PoultryHousesPage";
import ChicksLotsPage from "./modules/broiler-farm/pages/ChicksLotsPage";
import LoginPage from "./pages/LoginPage";
import { ProtectedRoute } from "./components/general/ProtectedRoute";
import SlaughterHouseHomePage from './modules/slaughter-house/pages/HomePage';
import ProductsPage from './modules/slaughter-house/pages/ProductsPage';
import SlaughterLotsPage from './modules/slaughter-house/pages/SlaughterLotsPage';
import ChickenReceptionsPage from './modules/slaughter-house/pages/ChickenReceptionsPage';

function App() {
  return (

    <AuthProvider>
      <ThemeProvider>
        <Routes>
          {/* Public Route - Login */}
          <Route path="/login" element={<LoginPage />} />

          {/* Protected Routes */}
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <Layout />
              </ProtectedRoute>
            }
          >
            <Route index element={<HomePage />} />
            <Route path="chicks-receptions" element={<ChicksReceptionsPage />} />
            <Route path="chicks-receptions/create" element={<CreateReceptionPage />} />
            <Route path="chicks-receptions/:id/edit" element={<CreateReceptionPage />} />
            <Route path="poultry-houses" element={<PoultryHousesPage />} />
            <Route path="chicks-lots" element={<ChicksLotsPage />} />
            <Route path="/slaughterhouse" element={<SlaughterHouseHomePage />} />
            <Route path="/slaughterhouse/products" element={<ProductsPage />} />
            <Route path="/slaughterhouse/lots" element={<SlaughterLotsPage />} />
            <Route path="/slaughterhouse/receptions" element={<ChickenReceptionsPage />} />
          </Route>

          {/* Catch-all redirect */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </ThemeProvider>
    </AuthProvider>

  );
}

export default App;