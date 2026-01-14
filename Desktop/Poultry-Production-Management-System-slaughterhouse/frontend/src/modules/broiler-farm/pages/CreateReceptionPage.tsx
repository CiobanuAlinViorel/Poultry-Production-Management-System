// src/pages/CreateReceptionPage.tsx
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router';
import { useReceptionStore, type Reception, type ReceptionLine } from '@/modules/broiler-farm/stores/useReceptionStore';
import { usePoultryHouseStore } from '@/modules/broiler-farm/stores/usePoultryHouseStore';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@/components/ui/table';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { ArrowLeft, Plus, Edit, Trash2, Save, Check, AlertCircle, Package, Truck, FileText, Calendar } from 'lucide-react';
import { format } from 'date-fns';

export default function CreateReceptionPage() {
    const navigate = useNavigate();
    const { user } = useAuth();
    const { createReception, updateReception, finalizeReception } = useReceptionStore();
    const { houses, fetchHouses, getAvailableHouses } = usePoultryHouseStore();

    // Auto-fill from logged user
    const [farmId, setFarmId] = useState('1');
    const [employeeId, setEmployeeId] = useState('');
    const [receptionDate, setReceptionDate] = useState(format(new Date(), "yyyy-MM-dd'T'HH:mm"));
    const [transportConditions, setTransportConditions] = useState('');
    const [truckInfo, setTruckInfo] = useState('');
    const [referenceDocument, setReferenceDocument] = useState('');
    const [lines, setLines] = useState<ReceptionLine[]>([]);

    // Dialog states
    const [lineDialogOpen, setLineDialogOpen] = useState(false);
    const [confirmDialogOpen, setConfirmDialogOpen] = useState(false);
    const [editingLineIndex, setEditingLineIndex] = useState<number | null>(null);

    // Line form state
    const [currentLine, setCurrentLine] = useState<ReceptionLine>({
        poultryHouseId: 0,
        chicksAlive: 0,
        chicksDOA: 0,
        chicksWeak: 0,
        qualityGrade: 'A',
        notes: '',
    });

    // Error handling
    const [error, setError] = useState<string | null>(null);
    const [savedReceptionId, setSavedReceptionId] = useState<number | null>(null);

    // Auto-fill employee and farm data on mount
    useEffect(() => {
        if (user) {
            setEmployeeId('10');
            setFarmId('1');
        }
    }, [user]);

    // Fetch available poultry houses when farm changes
    useEffect(() => {
        if (farmId) {
            fetchHouses(parseInt(farmId));
        }
    }, [farmId, fetchHouses]);

    // Get available houses for current farm
    const availableHouses = farmId ? getAvailableHouses(parseInt(farmId)) : [];

    const openAddLineDialog = () => {
        setCurrentLine({
            poultryHouseId: 0,
            chicksAlive: 0,
            chicksDOA: 0,
            chicksWeak: 0,
            qualityGrade: 'A',
            notes: '',
        });
        setEditingLineIndex(null);
        setLineDialogOpen(true);
    };

    const openEditLineDialog = (index: number) => {
        setCurrentLine({ ...lines[index] });
        setEditingLineIndex(index);
        setLineDialogOpen(true);
    };

    // Save line (add or update)
    const saveLine = () => {
        if (currentLine.poultryHouseId === 0) {
            setError('Please select a poultry house');
            return;
        }
        if (currentLine.chicksAlive <= 0) {
            setError('Chicks alive must be greater than 0');
            return;
        }

        // Verifică dacă hala e deja folosită în altă linie
        const isDuplicate = lines.some((line, idx) =>
            line.poultryHouseId === currentLine.poultryHouseId && idx !== editingLineIndex
        );

        if (isDuplicate) {
            setError('This poultry house is already used in another line');
            return;
        }

        // Calculează quantity
        const totalQuantity =
            (currentLine.chicksAlive || 0) +
            (currentLine.chicksDOA || 0) +
            (currentLine.chicksWeak || 0);

        const house = availableHouses.find(h => h.id === currentLine.poultryHouseId);

        const lineWithCalculations = {
            ...currentLine,
            quantity: totalQuantity,
            poultryHouseName: house?.id?.toString() || `House #${currentLine.poultryHouseId}`,
        };

        if (editingLineIndex !== null) {
            const updatedLines = [...lines];
            updatedLines[editingLineIndex] = lineWithCalculations;
            setLines(updatedLines);
        } else {
            setLines([...lines, lineWithCalculations]);
        }

        setLineDialogOpen(false);
        setError(null);
    };

    const removeLine = (index: number) => {
        setLines(lines.filter((_, i) => i !== index));
    };

    const handleSaveDraft = async () => {
        setError(null);

        if (!employeeId) {
            setError('Employee ID is required');
            return;
        }
        if (lines.length === 0) {
            setError('Please add at least one reception line');
            return;
        }

        try {
            const reception: Reception = {
                farmId: parseInt(farmId),
                employeeid: parseInt(employeeId),
                receptionDate,
                transportConditions,
                truckInfo,
                referenceDocument,
                lines,
            };

            let saved: Reception;

            if (savedReceptionId) {
                saved = await updateReception(savedReceptionId, reception);
            } else {
                saved = await createReception(reception);
            }

            setSavedReceptionId(saved.id || null);
            setConfirmDialogOpen(true);
        } catch (err: any) {
            setError(err.message || 'Failed to save reception');
        }
    };

    const handleFinalize = async () => {
        if (!savedReceptionId) {
            setError('Please save the draft first');
            return;
        }

        try {
            await finalizeReception(savedReceptionId);
            navigate('/chicks-receptions');
        } catch (err: any) {
            setError(err.message || 'Failed to finalize reception');
            setConfirmDialogOpen(false);
        }
    };

    const handleModify = () => {
        setConfirmDialogOpen(false);
    };

    const totalChicks = lines.reduce((sum, line) => sum + line.chicksAlive, 0);
    const totalDOA = lines.reduce((sum, line) => sum + line.chicksDOA, 0);
    const totalWeak = lines.reduce((sum, line) => sum + line.chicksWeak, 0);

    return (
        <div className="min-h-screen bg-bg pb-12">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
                {/* Header with gradient background */}
                <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-primary/10 via-primary/5 to-transparent border border-border shadow-sm">
                    <div className="absolute inset-0 bg-grid-pattern opacity-5"></div>
                    <div className="relative p-8">
                        <div className="flex items-start gap-6">
                            <Button
                                variant="ghost"
                                size="icon"
                                onClick={() => navigate('/chicks-receptions')}
                                className="mt-1 hover:bg-primary/10 rounded-xl"
                            >
                                <ArrowLeft className="h-5 w-5" />
                            </Button>
                            <div className="flex-1">
                                <div className="flex items-center gap-3 mb-2">
                                    <div className="p-2 bg-primary/10 rounded-xl">
                                        <Package className="h-6 w-6 text-primary" />
                                    </div>
                                    <h1 className="text-4xl font-bold text-text">
                                        {savedReceptionId ? 'Edit Reception' : 'New Chick Reception'}
                                    </h1>
                                </div>
                                <p className="text-text-muted text-lg">
                                    Record the arrival of new chicks and assign them to poultry houses
                                </p>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Error Alert */}
                {error && (
                    <Alert variant="destructive" className="rounded-xl border-2 shadow-sm animate-in fade-in slide-in-from-top-2">
                        <AlertCircle className="h-5 w-5" />
                        <AlertDescription className="font-medium">{error}</AlertDescription>
                    </Alert>
                )}

                {/* General Information */}
                <Card className="p-8 shadow-sm rounded-2xl border-border">
                    <div className="flex items-center gap-3 mb-6">
                        <div className="p-2 bg-primary/10 rounded-lg">
                            <FileText className="h-5 w-5 text-primary" />
                        </div>
                        <h2 className="text-2xl font-bold text-text">General Information</h2>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        {/* Farm (read-only) */}
                        <div className="space-y-2">
                            <Label className="text-sm font-semibold text-text">Farm</Label>
                            <Input
                                value={`Farm #${farmId}`}
                                disabled
                                className="bg-neutral-100 dark:bg-neutral-800/50 border-neutral-200 dark:border-neutral-700 rounded-lg h-11 font-medium"
                            />
                        </div>

                        {/* Employee (read-only) */}
                        <div className="space-y-2">
                            <Label className="text-sm font-semibold text-text">Receiving Employee</Label>
                            <Input
                                value={user?.username || 'Current User'}
                                disabled
                                className="bg-neutral-100 dark:bg-neutral-800/50 border-neutral-200 dark:border-neutral-700 rounded-lg h-11 font-medium"
                            />
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="receptionDate" className="text-sm font-semibold text-text flex items-center gap-2">
                                <Calendar className="h-4 w-4" />
                                Reception Date *
                            </Label>
                            <Input
                                id="receptionDate"
                                type="datetime-local"
                                value={receptionDate}
                                onChange={(e) => setReceptionDate(e.target.value)}
                                required
                                className="rounded-lg h-11 border-border focus:border-primary focus:ring-2 focus:ring-primary/20 bg-card"
                            />
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="referenceDocument" className="text-sm font-semibold text-text flex items-center gap-2">
                                <FileText className="h-4 w-4" />
                                Reference Document
                            </Label>
                            <Input
                                id="referenceDocument"
                                value={referenceDocument}
                                onChange={(e) => setReferenceDocument(e.target.value)}
                                placeholder="e.g., INV-2024-001"
                                className="rounded-lg h-11 border-border focus:border-primary focus:ring-2 focus:ring-primary/20 bg-card"
                            />
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="truckInfo" className="text-sm font-semibold text-text flex items-center gap-2">
                                <Truck className="h-4 w-4" />
                                Truck Information
                            </Label>
                            <Input
                                id="truckInfo"
                                value={truckInfo}
                                onChange={(e) => setTruckInfo(e.target.value)}
                                placeholder="e.g., B-123-ABC, Driver: John Doe"
                                className="rounded-lg h-11 border-border focus:border-primary focus:ring-2 focus:ring-primary/20 bg-card"
                            />
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="transportConditions" className="text-sm font-semibold text-text">
                                Transport Conditions
                            </Label>
                            <Textarea
                                id="transportConditions"
                                value={transportConditions}
                                onChange={(e) => setTransportConditions(e.target.value)}
                                placeholder="e.g., Temperature 22°C, Good ventilation"
                                rows={3}
                                className="rounded-lg border-border focus:border-primary focus:ring-2 focus:ring-primary/20 resize-none bg-card"
                            />
                        </div>
                    </div>
                </Card>

                {/* Reception Lines */}
                <Card className="p-8 shadow-sm rounded-2xl border-border">
                    <div className="flex items-center justify-between mb-6">
                        <div className="flex items-center gap-3">
                            <div className="p-2 bg-primary/10 rounded-lg">
                                <Package className="h-5 w-5 text-primary" />
                            </div>
                            <h2 className="text-2xl font-bold text-text">Reception Lines</h2>
                        </div>
                        <Button
                            onClick={openAddLineDialog}
                            className="gap-2 rounded-xl shadow-sm bg-primary hover:bg-primary-hover text-neutral-50 transition-all hover:shadow-md"
                        >
                            <Plus className="h-4 w-4" />
                            Add Line
                        </Button>
                    </div>

                    {lines.length === 0 ? (
                        <div className="text-center py-16 border-2 border-dashed border-border rounded-2xl bg-neutral-50/50 dark:bg-neutral-800/20">
                            <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-primary/10 mb-4">
                                <Package className="h-8 w-8 text-primary" />
                            </div>
                            <p className="text-text-muted text-lg font-medium">No lines added yet</p>
                            <p className="text-text-muted text-sm mt-1">Click "Add Line" to get started</p>
                        </div>
                    ) : (
                        <>
                            <div className="rounded-xl border border-border overflow-hidden bg-card">
                                <Table>
                                    <TableHeader>
                                        <TableRow className="bg-neutral-50 dark:bg-neutral-800/50 hover:bg-neutral-50 dark:hover:bg-neutral-800/50">
                                            <TableHead className="font-bold text-text">Poultry House</TableHead>
                                            <TableHead className="font-bold text-text">Chicks Alive</TableHead>
                                            <TableHead className="font-bold text-text">DOA</TableHead>
                                            <TableHead className="font-bold text-text">Weak</TableHead>
                                            <TableHead className="font-bold text-text">Grade</TableHead>
                                            <TableHead className="font-bold text-text">Notes</TableHead>
                                            <TableHead className="font-bold text-text w-[100px]">Actions</TableHead>
                                        </TableRow>
                                    </TableHeader>
                                    <TableBody>
                                        {lines.map((line, index) => (
                                            <TableRow key={index} className="hover:bg-neutral-50/50 dark:hover:bg-neutral-800/30">
                                                <TableCell className="font-semibold text-text">{line.poultryHouseName}</TableCell>
                                                <TableCell className="font-medium text-green-700 dark:text-green-400">
                                                    {line.chicksAlive.toLocaleString()}
                                                </TableCell>
                                                <TableCell className="font-medium text-red-600 dark:text-red-400">
                                                    {line.chicksDOA}
                                                </TableCell>
                                                <TableCell className="font-medium text-yellow-600 dark:text-yellow-400">
                                                    {line.chicksWeak}
                                                </TableCell>
                                                <TableCell>
                                                    <span className="inline-flex items-center px-3 py-1 bg-primary/10 text-primary rounded-full text-sm font-semibold">
                                                        {line.qualityGrade}
                                                    </span>
                                                </TableCell>
                                                <TableCell className="text-text-muted text-sm">{line.notes || '—'}</TableCell>
                                                <TableCell>
                                                    <div className="flex gap-1">
                                                        <Button
                                                            variant="ghost"
                                                            size="icon"
                                                            onClick={() => openEditLineDialog(index)}
                                                            className="rounded-lg hover:bg-primary/10"
                                                        >
                                                            <Edit className="h-4 w-4" />
                                                        </Button>
                                                        <Button
                                                            variant="ghost"
                                                            size="icon"
                                                            onClick={() => removeLine(index)}
                                                            className="text-red-600 hover:text-red-700 hover:bg-red-50 dark:hover:bg-red-950 rounded-lg"
                                                        >
                                                            <Trash2 className="h-4 w-4" />
                                                        </Button>
                                                    </div>
                                                </TableCell>
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
                            </div>

                            {/* Summary Cards */}
                            <div className="mt-6 grid grid-cols-1 md:grid-cols-3 gap-4">
                                <Card className="p-6 bg-gradient-to-br from-green-50 to-green-100/50 dark:from-green-950/50 dark:to-green-900/30 border-green-200 dark:border-green-800 rounded-xl shadow-sm">
                                    <div className="flex items-center justify-between">
                                        <div>
                                            <div className="text-sm font-semibold text-green-800 dark:text-green-200 mb-1">
                                                Total Alive
                                            </div>
                                            <div className="text-3xl font-bold text-green-900 dark:text-green-100">
                                                {totalChicks.toLocaleString()}
                                            </div>
                                        </div>
                                        <div className="p-3 bg-green-200/50 dark:bg-green-800/50 rounded-xl">
                                            <Check className="h-6 w-6 text-green-700 dark:text-green-300" />
                                        </div>
                                    </div>
                                </Card>

                                <Card className="p-6 bg-gradient-to-br from-red-50 to-red-100/50 dark:from-red-950/50 dark:to-red-900/30 border-red-200 dark:border-red-800 rounded-xl shadow-sm">
                                    <div className="flex items-center justify-between">
                                        <div>
                                            <div className="text-sm font-semibold text-red-800 dark:text-red-200 mb-1">
                                                Total DOA
                                            </div>
                                            <div className="text-3xl font-bold text-red-900 dark:text-red-100">
                                                {totalDOA}
                                            </div>
                                        </div>
                                        <div className="p-3 bg-red-200/50 dark:bg-red-800/50 rounded-xl">
                                            <AlertCircle className="h-6 w-6 text-red-700 dark:text-red-300" />
                                        </div>
                                    </div>
                                </Card>

                                <Card className="p-6 bg-gradient-to-br from-yellow-50 to-yellow-100/50 dark:from-yellow-950/50 dark:to-yellow-900/30 border-yellow-200 dark:border-yellow-800 rounded-xl shadow-sm">
                                    <div className="flex items-center justify-between">
                                        <div>
                                            <div className="text-sm font-semibold text-yellow-800 dark:text-yellow-200 mb-1">
                                                Total Weak
                                            </div>
                                            <div className="text-3xl font-bold text-yellow-900 dark:text-yellow-100">
                                                {totalWeak}
                                            </div>
                                        </div>
                                        <div className="p-3 bg-yellow-200/50 dark:bg-yellow-800/50 rounded-xl">
                                            <AlertCircle className="h-6 w-6 text-yellow-700 dark:text-yellow-300" />
                                        </div>
                                    </div>
                                </Card>
                            </div>
                        </>
                    )}
                </Card>

                {/* Actions - Sticky bottom on mobile */}
                <div className="flex flex-col sm:flex-row justify-end gap-3 sticky bottom-4 bg-bg/80 backdrop-blur-sm p-4 rounded-2xl border border-border shadow-lg">
                    <Button
                        variant="outline"
                        onClick={() => navigate('/chicks-receptions')}
                        className="rounded-xl border-border hover:bg-neutral-100 dark:hover:bg-neutral-800"
                    >
                        Cancel
                    </Button>
                    <Button
                        onClick={handleSaveDraft}
                        className="gap-2 rounded-xl bg-primary hover:bg-primary-hover text-neutral-50 shadow-sm transition-all hover:shadow-md"
                    >
                        <Save className="h-4 w-4" />
                        Save Draft
                    </Button>
                </div>
            </div>

            {/* Add/Edit Line Dialog */}
            <Dialog open={lineDialogOpen} onOpenChange={setLineDialogOpen}>
                <DialogContent className="max-w-2xl bg-card rounded-2xl border-border">
                    <DialogHeader>
                        <DialogTitle className="text-2xl font-bold text-text">
                            {editingLineIndex !== null ? 'Edit Reception Line' : 'Add Reception Line'}
                        </DialogTitle>
                        <DialogDescription className="text-text-muted">
                            Enter the details for this reception line
                        </DialogDescription>
                    </DialogHeader>

                    <div className="space-y-5 py-4">
                        {/* Poultry House */}
                        <div className="space-y-2">
                            <Label htmlFor="poultryHouseId" className="text-sm font-semibold text-text">
                                Poultry House *
                            </Label>
                            <Select
                                value={currentLine.poultryHouseId === 0 ? undefined : currentLine.poultryHouseId.toString()}
                                onValueChange={(value) =>
                                    setCurrentLine({ ...currentLine, poultryHouseId: parseInt(value) })
                                }
                            >
                                <SelectTrigger className="h-11 rounded-lg border-border focus:border-primary focus:ring-2 focus:ring-primary/20 bg-card">
                                    <SelectValue placeholder="Select a house" />
                                </SelectTrigger>
                                <SelectContent className="rounded-lg bg-card border-border">
                                    {availableHouses.length === 0 ? (
                                        <SelectItem value="0" disabled>
                                            No available houses
                                        </SelectItem>
                                    ) : (
                                        availableHouses.map((house) =>
                                            house.id !== undefined && (
                                                <SelectItem key={house.id} value={house.id.toString()}>
                                                    House {house.id} (Capacity: {house.capacity.toLocaleString()})
                                                </SelectItem>
                                            )
                                        )
                                    )}
                                </SelectContent>
                            </Select>
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                            <div className="space-y-2">
                                <Label htmlFor="chicksAlive" className="text-sm font-semibold text-text">
                                    Chicks Alive *
                                </Label>
                                <Input
                                    id="chicksAlive"
                                    type="number"
                                    value={currentLine.chicksAlive || ''}
                                    onChange={(e) =>
                                        setCurrentLine({ ...currentLine, chicksAlive: parseInt(e.target.value) || 0 })
                                    }
                                    className="h-11 rounded-lg border-border focus:border-primary focus:ring-2 focus:ring-primary/20 bg-card"
                                />
                            </div>
                            <div className="space-y-2">
                                <Label htmlFor="chicksDOA" className="text-sm font-semibold text-text">
                                    DOA (Dead on Arrival)
                                </Label>
                                <Input
                                    id="chicksDOA"
                                    type="number"
                                    value={currentLine.chicksDOA || ''}
                                    onChange={(e) =>
                                        setCurrentLine({ ...currentLine, chicksDOA: parseInt(e.target.value) || 0 })
                                    }
                                    className="h-11 rounded-lg border-border focus:border-primary focus:ring-2 focus:ring-primary/20 bg-card"
                                />
                            </div>
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                            <div className="space-y-2">
                                <Label htmlFor="breed" className="text-sm font-semibold text-text">
                                    Breed *
                                </Label>
                                <Input
                                    id="breed"
                                    type="text"
                                    value={currentLine.breed || ''}
                                    onChange={(e) =>
                                        setCurrentLine({ ...currentLine, breed: e.target.value || '' })
                                    }
                                    placeholder="e.g., Ross 308"
                                    className="h-11 rounded-lg border-border focus:border-primary focus:ring-2 focus:ring-primary/20 bg-card"
                                />
                            </div>
                            <div className="space-y-2">
                                <Label htmlFor="hatcherySource" className="text-sm font-semibold text-text">
                                    Hatchery Source
                                </Label>
                                <Input
                                    id="hatcherySource"
                                    type="text"
                                    value={currentLine.hatcherySource || ''}
                                    onChange={(e) =>
                                        setCurrentLine({ ...currentLine, hatcherySource: e.target.value || '' })
                                    }
                                    placeholder="e.g., ABC Hatchery"
                                    className="h-11 rounded-lg border-border focus:border-primary focus:ring-2 focus:ring-primary/20 bg-card"
                                />
                            </div>
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                            <div className="space-y-2">
                                <Label htmlFor="chicksWeak" className="text-sm font-semibold text-text">
                                    Weak Chicks
                                </Label>
                                <Input
                                    id="chicksWeak"
                                    type="number"
                                    value={currentLine.chicksWeak || ''}
                                    onChange={(e) =>
                                        setCurrentLine({ ...currentLine, chicksWeak: parseInt(e.target.value) || 0 })
                                    }
                                    className="h-11 rounded-lg border-border focus:border-primary focus:ring-2 focus:ring-primary/20 bg-card"
                                />
                            </div>
                            <div className="space-y-2">
                                <Label htmlFor="qualityGrade" className="text-sm font-semibold text-text">
                                    Quality Grade
                                </Label>
                                <Select
                                    value={currentLine.qualityGrade}
                                    onValueChange={(value) => setCurrentLine({ ...currentLine, qualityGrade: value as 'A' | 'B' | 'C' | 'D' | 'F' })}
                                >
                                    <SelectTrigger className="h-11 rounded-lg bg-card">
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent className="rounded-lg bg-card border-border">
                                        <SelectItem value="A">A - Excellent</SelectItem>
                                        <SelectItem value="B">B - Good</SelectItem>
                                        <SelectItem value="C">C - Average</SelectItem>
                                        <SelectItem value="D">D - Poor</SelectItem>
                                    </SelectContent>
                                </Select>
                            </div>
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="notes" className="text-sm font-semibold text-text">
                                Notes
                            </Label>
                            <Textarea
                                id="notes"
                                value={currentLine.notes}
                                onChange={(e) => setCurrentLine({ ...currentLine, notes: e.target.value })}
                                placeholder="Additional observations"
                                rows={3}
                                className="rounded-lg border-border focus:border-primary focus:ring-2 focus:ring-primary/20 resize-none bg-card"
                            />
                        </div>
                    </div>

                    <DialogFooter>
                        <Button
                            variant="outline"
                            onClick={() => setLineDialogOpen(false)}
                            className="rounded-lg"
                        >
                            Cancel
                        </Button>
                        <Button
                            onClick={saveLine}
                            className="rounded-lg bg-primary hover:bg-primary-hover text-neutral-50"
                        >
                            {editingLineIndex !== null ? 'Update' : 'Add'} Line
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            {/* Confirm Dialog */}
            <AlertDialog open={confirmDialogOpen} onOpenChange={setConfirmDialogOpen}>
                <AlertDialogContent className="bg-card rounded-2xl border-border">
                    <AlertDialogHeader>
                        <div className="flex items-center gap-3 mb-2">
                            <div className="p-2 bg-green-100 dark:bg-green-900 rounded-lg">
                                <Check className="h-6 w-6 text-green-600 dark:text-green-400" />
                            </div>
                            <AlertDialogTitle className="text-2xl">Reception Saved!</AlertDialogTitle>
                        </div>
                        <AlertDialogDescription className="text-base">
                            The reception has been saved as a draft. Would you like to finalize it now or continue making changes?
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel
                            onClick={handleModify}
                            className="rounded-lg"
                        >
                            Continue Editing
                        </AlertDialogCancel>
                        <AlertDialogAction
                            onClick={handleFinalize}
                            className="bg-primary hover:bg-primary-hover text-neutral-50 gap-2 rounded-lg shadow-sm"
                        >
                            <Check className="h-4 w-4" />
                            Finalize Reception
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </div>
    );
}