// src/components/general/Sidebar.tsx
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";
import { Badge } from "@/components/ui/badge";
import { useTheme } from "./ThemeProvider";
import { useAuth } from "@/contexts/AuthContext";
import {
    Home,
    Bird,
    Building2,
    Package,
    Moon,
    Sun,
    Menu,
    LogOut,
    Beef,
    Truck,
    Scissors,
    ClipboardList,
} from "lucide-react";
import { useState } from "react";
import { cn } from "@/lib/utils";
import { Link, useLocation, useNavigate } from "react-router";

// 🐔 BROILER FARM NAVIGATION
const broilerFarmNavigation = [
    {
        name: "Dashboard",
        href: "/",
        icon: Home,
    },
    {
        name: "Chicks Receptions",
        href: "/chicks-receptions",
        icon: Bird,
    },
    {
        name: "Poultry Houses",
        href: "/poultry-houses",
        icon: Building2,
    },
    {
        name: "Chicks Lots",
        href: "/chicks-lots",
        icon: Package,
    },
];

// 🔪 SLAUGHTER HOUSE NAVIGATION
const slaughterHouseNavigation = [
    {
        name: "Dashboard",
        href: "/",
        icon: Home,
    },
    {
        name: "Livestock Reception",
        href: "/livestock-reception",
        icon: Truck,
    },
    {
        name: "Slaughter Process",
        href: "/slaughter-process",
        icon: Scissors,
    },
    {
        name: "Production",
        href: "/production",
        icon: Beef,
    },
    {
        name: "Quality Control",
        href: "/quality-control",
        icon: ClipboardList,
    },
];

export function Sidebar() {
    const location = useLocation();
    const navigate = useNavigate();
    const { theme, toggleTheme } = useTheme();
    const { user, logout, currentModule } = useAuth();
    const [isOpen, setIsOpen] = useState(false);

    const handleLogout = async () => {
        await logout();
        navigate('/login');
    };

    // ✨ Select navigation based on current module
    const navigation = currentModule === 'broiler-farm'
        ? broilerFarmNavigation
        : slaughterHouseNavigation;

    // ✨ Module configuration
    const moduleConfig = currentModule === 'broiler-farm'
        ? {
            name: "Broiler Farm",
            icon: Bird,
            color: "text-green-600 dark:text-green-400",
            bgColor: "bg-green-100 dark:bg-green-900/30",
            borderColor: "border-green-200 dark:border-green-800",
            badgeBg: "bg-green-50 dark:bg-green-950/50",
        }
        : {
            name: "Slaughter House",
            icon: Beef,
            color: "text-red-600 dark:text-red-400",
            bgColor: "bg-red-100 dark:bg-red-900/30",
            borderColor: "border-red-200 dark:border-red-800",
            badgeBg: "bg-red-50 dark:bg-red-950/50",
        };

    // Extract initials for avatar
    const getInitials = (username: string) => {
        return username
            .split(' ')
            .map(n => n[0])
            .join('')
            .toUpperCase()
            .slice(0, 2);
    };

    return (
        <>
            {/* Mobile menu button */}
            <Button
                variant="outline"
                size="icon"
                onClick={() => setIsOpen(!isOpen)}
                className="lg:hidden fixed top-4 left-4 z-50 bg-card shadow-lg"
            >
                <Menu className="h-5 w-5" />
            </Button>

            {/* Mobile backdrop */}
            {isOpen && (
                <div
                    className="fixed inset-0 bg-black/50 z-40 lg:hidden animate-in fade-in"
                    onClick={() => setIsOpen(false)}
                />
            )}

            {/* Sidebar */}
            <aside
                className={cn(
                    "fixed left-0 top-0 z-40 h-screen w-64 bg-card border-r border-border shadow-xl transition-transform duration-300 ease-in-out lg:translate-x-0",
                    isOpen ? "translate-x-0" : "-translate-x-full"
                )}
            >
                <div className="flex h-full flex-col">
                    {/* Header with Module Badge */}
                    <div className="border-b border-border px-6 py-4">
                        <div className="flex items-center justify-between mb-3">
                            <div className="flex items-center gap-2">
                                <div className={cn(
                                    "flex items-center justify-center w-10 h-10 rounded-lg",
                                    moduleConfig.bgColor
                                )}>
                                    <moduleConfig.icon className={cn("h-5 w-5", moduleConfig.color)} />
                                </div>
                                <div className="flex flex-col">
                                    <span className="text-sm font-semibold text-text leading-none">
                                        Poultry Manager
                                    </span>
                                    <span className="text-xs text-text-muted mt-1">
                                        v1.0.0
                                    </span>
                                </div>
                            </div>
                        </div>

                        {/* Module Badge */}
                        <Badge
                            variant="outline"
                            className={cn(
                                "w-full justify-center py-1.5 font-medium",
                                moduleConfig.borderColor,
                                moduleConfig.color,
                                moduleConfig.badgeBg
                            )}
                        >
                            <moduleConfig.icon className="h-3 w-3 mr-1.5" />
                            {moduleConfig.name}
                        </Badge>
                    </div>

                    {/* Navigation */}
                    <ScrollArea className="flex-1 py-4">
                        <nav className="space-y-1 px-3">
                            {navigation.map((item) => {
                                const isActive = location.pathname === item.href;
                                const Icon = item.icon;

                                return (
                                    <Link
                                        key={item.name}
                                        to={item.href}
                                        onClick={() => setIsOpen(false)}
                                        className={cn(
                                            "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-all duration-200",
                                            isActive
                                                ? "bg-primary text-neutral-50 shadow-md scale-[0.98]"
                                                : "text-text-muted hover:bg-neutral-100 dark:hover:bg-neutral-800 hover:text-text hover:scale-[0.98]"
                                        )}
                                    >
                                        <Icon className="h-5 w-5 shrink-0" />
                                        <span>{item.name}</span>
                                    </Link>
                                );
                            })}
                        </nav>
                    </ScrollArea>

                    {/* Footer */}
                    <div className="p-4 space-y-4">
                        <Separator />

                        {/* User Info */}
                        <div className="px-3 py-2 rounded-lg bg-neutral-100 dark:bg-neutral-800">
                            <div className="flex items-center gap-2 mb-2">
                                <div className="w-8 h-8 rounded-full bg-primary/20 flex items-center justify-center">
                                    <span className="text-xs font-semibold text-primary">
                                        {user ? getInitials(user.username) : 'U'}
                                    </span>
                                </div>
                                <div className="flex flex-col flex-1">
                                    <span className="text-xs font-medium text-text truncate max-w-[120px]">
                                        {user?.username || 'User'}
                                    </span>
                                    <span className="text-xs text-text-muted">
                                        {user?.roles?.includes('ADMIN') ? 'Admin' :
                                            user?.roles?.includes('MANAGER') ? 'Manager' : 'Employee'}
                                    </span>
                                </div>
                            </div>

                            {/* Employee/Farm ID Badges */}
                            {user && (
                                <div className="flex gap-2 text-xs flex-wrap">
                                    {user.farmId && (
                                        <Badge variant="secondary" className="text-xs py-0 px-2">
                                            Farm #{user.farmId}
                                        </Badge>
                                    )}
                                    {user.employeeId && (
                                        <Badge variant="secondary" className="text-xs py-0 px-2">
                                            Emp #{user.employeeId}
                                        </Badge>
                                    )}
                                </div>
                            )}
                        </div>

                        {/* Theme Toggle */}
                        <Button
                            variant="outline"
                            size="default"
                            onClick={toggleTheme}
                            className="w-full justify-start gap-3"
                        >
                            {theme === "light" ? (
                                <>
                                    <Moon className="h-5 w-5" />
                                    <span>Dark Mode</span>
                                </>
                            ) : (
                                <>
                                    <Sun className="h-5 w-5" />
                                    <span>Light Mode</span>
                                </>
                            )}
                        </Button>

                        {/* Logout Button */}
                        <Button
                            variant="destructive"
                            size="default"
                            onClick={handleLogout}
                            className="w-full justify-start gap-3"
                        >
                            <LogOut className="h-5 w-5" />
                            <span>Logout</span>
                        </Button>
                    </div>
                </div>
            </aside>
        </>
    );
}