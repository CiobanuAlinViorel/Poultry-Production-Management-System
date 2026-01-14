// src/pages/LoginPage.tsx
import { useState } from 'react';
import { useNavigate } from 'react-router';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import {
    Bird,
    AlertCircle,
    Loader2,
    Beef,
    ArrowLeft,
    Check,
    Lock
} from 'lucide-react';
import { cn } from '@/lib/utils';

type Module = 'broiler-farm' | 'slaughter-house';

export default function LoginPage() {
    const navigate = useNavigate();
    const { login } = useAuth();

    // States
    const [selectedModule, setSelectedModule] = useState<Module | null>(null);
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    const modules = [
        {
            id: 'broiler-farm' as Module,
            name: 'Broiler Farm',
            description: 'Manage chick receptions, poultry houses, and lots',
            icon: Bird,
            gradient: 'from-green-500 to-emerald-600',
            bgGradient: 'from-green-50 to-emerald-50 dark:from-green-950/30 dark:to-emerald-900/30',
            borderColor: 'border-green-200 dark:border-green-800',
            iconBg: 'bg-green-100 dark:bg-green-900/50',
            iconColor: 'text-green-600 dark:text-green-400',
            available: true,
        },
        {
            id: 'slaughter-house' as Module,
            name: 'Slaughter House',
            description: 'Manage livestock reception, slaughter, and production',
            icon: Beef,
            gradient: 'from-red-500 to-rose-600',
            bgGradient: 'from-red-50 to-rose-50 dark:from-red-950/30 dark:to-rose-900/30',
            borderColor: 'border-red-200 dark:border-red-800',
            iconBg: 'bg-red-100 dark:bg-red-900/50',
            iconColor: 'text-red-600 dark:text-red-400',
            available: true, // ✅ NOW AVAILABLE!  ← DOAR ASTA!
        },
    ];

    const handleModuleSelect = (moduleId: Module) => {
        const module = modules.find(m => m.id === moduleId);
        if (module?.available) {
            setSelectedModule(moduleId);
            setError('');
        }
    };

    const handleBack = () => {
        setSelectedModule(null);
        setEmail('');
        setPassword('');
        setError('');
    };

const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedModule) return;

    setError('');
    setIsLoading(true);

    try {
        await login(email, password, selectedModule);

        // ⭐ Redirect based on selected module
        if (selectedModule === 'slaughter-house') {
            navigate('/slaughterhouse');
        } else {
            navigate('/');
        }

    } catch (err: any) {
        setError(err.message || 'Login failed. Please try again.');
    } finally {
        setIsLoading(false);
    }
};

    // Module Selection Screen
    if (!selectedModule) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-neutral-100 via-bg to-neutral-100 dark:from-neutral-900 dark:via-bg dark:to-neutral-900 p-4">
                <div className="w-full max-w-4xl">
                    {/* Header */}
                    <div className="text-center mb-8">
                        <div className="inline-flex items-center justify-center w-20 h-20 rounded-2xl bg-primary/10 mb-4">
                            <Bird className="h-10 w-10 text-primary" />
                        </div>
                        <h1 className="text-4xl font-bold text-text mb-2">
                            Poultry Manager
                        </h1>
                        <p className="text-text-muted text-lg">
                            Select your module to continue
                        </p>
                    </div>

                    {/* Module Cards */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
                        {modules.map((module) => {
                            const Icon = module.icon;
                            const isDisabled = !module.available;

                            return (
                                <Card
                                    key={module.id}
                                    onClick={() => !isDisabled && handleModuleSelect(module.id)}
                                    className={cn(
                                        "relative overflow-hidden cursor-pointer transition-all duration-300",
                                        isDisabled
                                            ? "opacity-50 cursor-not-allowed"
                                            : "hover:shadow-xl hover:scale-[1.02] active:scale-[0.98]",
                                        module.borderColor
                                    )}
                                >
                                    {/* Background gradient */}
                                    <div className={cn(
                                        "absolute inset-0 bg-gradient-to-br opacity-50",
                                        module.bgGradient
                                    )} />

                                    <div className="relative p-8">
                                        {/* Icon & Badge */}
                                        <div className="flex items-start justify-between mb-6">
                                            <div className={cn(
                                                "p-4 rounded-2xl",
                                                module.iconBg
                                            )}>
                                                <Icon className={cn("h-8 w-8", module.iconColor)} />
                                            </div>
                                            {isDisabled ? (
                                                <Badge variant="outline" className="bg-neutral-100 dark:bg-neutral-800 border-neutral-300 dark:border-neutral-700">
                                                    <Lock className="h-3 w-3 mr-1" />
                                                    Coming Soon
                                                </Badge>
                                            ) : (
                                                <Badge variant="outline" className={cn(
                                                    "border-2",
                                                    module.borderColor,
                                                    module.iconColor
                                                )}>
                                                    Available
                                                </Badge>
                                            )}
                                        </div>

                                        {/* Content */}
                                        <h3 className="text-2xl font-bold text-text mb-2">
                                            {module.name}
                                        </h3>
                                        <p className="text-text-muted text-sm leading-relaxed mb-6">
                                            {module.description}
                                        </p>

                                        {/* Status Message */}
                                        {isDisabled ? (
                                            <div className="flex items-center gap-2 text-sm text-text-muted">
                                                <Lock className="h-4 w-4" />
                                                <span>Module under development</span>
                                            </div>
                                        ) : (
                                            <Button
                                                className={cn(
                                                    "w-full bg-gradient-to-r text-neutral-50",
                                                    module.gradient
                                                )}
                                            >
                                                Select Module
                                                <Check className="h-4 w-4 ml-2" />
                                            </Button>
                                        )}
                                    </div>
                                </Card>
                            );
                        })}
                    </div>

                    {/* Version */}
                    <p className="text-center text-sm text-text-muted">
                        Poultry Manager v1.0.0
                    </p>
                </div>
            </div>
        );
    }

    // Login Form (after module selection)
    const selectedModuleData = modules.find(m => m.id === selectedModule)!;
    const Icon = selectedModuleData.icon;

    return (
        <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-neutral-100 via-bg to-neutral-100 dark:from-neutral-900 dark:via-bg dark:to-neutral-900 p-4">
            <div className="w-full max-w-md">
                {/* Back Button */}
                <Button
                    variant="ghost"
                    onClick={handleBack}
                    className="mb-4 hover:bg-neutral-100 dark:hover:bg-neutral-800"
                >
                    <ArrowLeft className="h-4 w-4 mr-2" />
                    Change Module
                </Button>

                {/* Login Card */}
                <div className="bg-card rounded-2xl shadow-2xl border border-border overflow-hidden">
                    {/* Header with selected module */}
                    <div className={cn(
                        "bg-gradient-to-r p-8 text-white",
                        selectedModuleData.gradient
                    )}>
                        <div className="flex items-center justify-center mb-4">
                            <div className="w-16 h-16 bg-white/10 backdrop-blur-sm rounded-full flex items-center justify-center">
                                <Icon className="h-8 w-8 text-white" />
                            </div>
                        </div>
                        <h1 className="text-2xl font-bold text-center mb-1">
                            {selectedModuleData.name}
                        </h1>
                        <p className="text-white/80 text-sm text-center">
                            Sign in to continue
                        </p>
                    </div>

                    {/* Form */}
                    <div className="p-8">
                        <form onSubmit={handleSubmit} className="space-y-6">
                            {/* Error Alert */}
                            {error && (
                                <Alert variant="destructive" className="animate-in fade-in slide-in-from-top-2">
                                    <AlertCircle className="h-4 w-4" />
                                    <AlertDescription>{error}</AlertDescription>
                                </Alert>
                            )}

                            {/* Email */}
                            <div className="space-y-2">
                                <Label htmlFor="email" className="text-sm font-medium text-text">
                                    Email
                                </Label>
                                <Input
                                    id="email"
                                    type="email"
                                    placeholder="admin@farm.com"
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    required
                                    className="h-11 bg-card"
                                    disabled={isLoading}
                                />
                            </div>

                            {/* Password */}
                            <div className="space-y-2">
                                <Label htmlFor="password" className="text-sm font-medium text-text">
                                    Password
                                </Label>
                                <Input
                                    id="password"
                                    type="password"
                                    placeholder="••••••••"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    required
                                    className="h-11 bg-card"
                                    disabled={isLoading}
                                />
                            </div>

                            {/* Submit Button */}
                            <Button
                                type="submit"
                                className={cn(
                                    "w-full h-11 text-base font-medium bg-gradient-to-r text-neutral-50",
                                    selectedModuleData.gradient
                                )}
                                disabled={isLoading}
                            >
                                {isLoading ? (
                                    <>
                                        <Loader2 className="mr-2 h-5 w-5 animate-spin" />
                                        Signing in...
                                    </>
                                ) : (
                                    'Sign In'
                                )}
                            </Button>
                        </form>

                        {/* Demo Credentials */}
                        <div className="mt-6 pt-6 border-t border-border">
                            <p className="text-center text-sm text-text-muted">
                                Demo credentials: <br />
                                <span className="font-mono text-xs bg-neutral-100 dark:bg-neutral-800 px-2 py-1 rounded">
                                    admin@farm.com / admin123
                                </span>
                            </p>
                        </div>
                    </div>
                </div>

                {/* Version */}
                <p className="text-center text-sm text-text-muted mt-6">
                    Poultry Manager v1.0.0
                </p>
            </div>
        </div>
    );
}