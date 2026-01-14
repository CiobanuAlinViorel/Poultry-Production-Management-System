import { Outlet } from "react-router";
import { Sidebar } from "@/components/general/Sidebar";

export function Layout() {
    return (
        <div className="min-h-screen bg-bg">
            <Sidebar />
            <main className="lg:pl-64">
                <div className="container mx-auto p-6 lg:p-8">
                    <Outlet />
                </div>
            </main>
        </div>
    );
}