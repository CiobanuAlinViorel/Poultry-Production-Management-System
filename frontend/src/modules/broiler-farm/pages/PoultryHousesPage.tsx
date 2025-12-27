// src/pages/PoultryHousesPage.tsx
import { useEffect, useState } from 'react';
import { usePoultryHouseStore, type PoultryHouse } from '@/modules/broiler-farm/stores/usePoultryHouseStore';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select';
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@/components/ui/table';
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
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Plus, MoreVertical, Edit, Trash2, Loader2, AlertCircle, Building2 } from 'lucide-react';

// Type enums matching backend
type PoultryHouseType = 'OPEN' | 'CLOSED' | 'SEMI_CLOSED';
type PoultryHouseStatus = 'EMPTY' | 'OCCUPIED' | 'MAINTENANCE';

export default function PoultryHousesPage() {
    const { user } = useAuth();
    const { houses, isLoading, error, fetchHouses, createHouse, updateHouse, deleteHouse, clearError } = usePoultryHouseStore();

    console.log(user)

    // Dialog states
    const [formDialogOpen, setFormDialogOpen] = useState(false);
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
    const [editingHouse, setEditingHouse] = useState<PoultryHouse | null>(null);
    const [deletingHouseId, setDeletingHouseId] = useState<number | null>(null);

    // Form state
    const [formData, setFormData] = useState<PoultryHouse>({
        farmId: user?.farmId || 1,
        capacity: 0,
        area: 0,
        type: 'CLOSED' as PoultryHouseType,
        equipmentType: '',
        status: 'EMPTY' as PoultryHouseStatus,
        currentOccupancy: 0,
    });

    useEffect(() => {
        if (user?.farmId) {
            fetchHouses(user.farmId);
        } else {
            fetchHouses();
        }
    }, [user?.farmId, fetchHouses]);

    const openCreateDialog = () => {
        setEditingHouse(null);
        setFormData({
            farmId: user?.farmId || 1,
            capacity: 0,
            area: 0,
            type: 'CLOSED' as PoultryHouseType,
            equipmentType: '',
            status: 'EMPTY' as PoultryHouseStatus,
            currentOccupancy: 0,
        });
        setFormDialogOpen(true);
    };

    const openEditDialog = (house: PoultryHouse) => {
        setEditingHouse(house);
        setFormData({ ...house });
        setFormDialogOpen(true);
    };

    const openDeleteDialog = (id: number) => {
        setDeletingHouseId(id);
        setDeleteDialogOpen(true);
    };

    const handleSubmit = async () => {
        try {
            clearError();

            if (formData.capacity <= 0) {
                return;
            }

            if (editingHouse?.id) {
                await updateHouse(editingHouse.id, formData);
            } else {
                await createHouse(formData);
            }

            setFormDialogOpen(false);
        } catch (err) {
            console.error('Failed to save house:', err);
        }
    };

    const handleDelete = async () => {
        if (deletingHouseId) {
            try {
                await deleteHouse(deletingHouseId);
                setDeleteDialogOpen(false);
                setDeletingHouseId(null);
            } catch (err) {
                console.error('Failed to delete house:', err);
            }
        }
    };

    const getStatusBadge = (status: string) => {
        const config = {
            EMPTY: { variant: 'outline' as const, className: 'text-green-600 border-green-600' },
            OCCUPIED: { variant: 'default' as const, className: 'bg-blue-600' },
            MAINTENANCE: { variant: 'secondary' as const, className: 'bg-yellow-600 text-white' },
        };
        const { variant, className } = config[status as keyof typeof config] || config.EMPTY;

        return (
            <Badge variant={variant} className={className}>
                {status}
            </Badge>
        );
    };

    const getTypeBadge = (type: string) => {
        const config = {
            OPEN: 'bg-sky-100 text-sky-800 dark:bg-sky-900 dark:text-sky-200',
            CLOSED: 'bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-200',
            SEMI_CLOSED: 'bg-indigo-100 text-indigo-800 dark:bg-indigo-900 dark:text-indigo-200',
        };

        return (
            <Badge variant="outline" className={config[type as keyof typeof config]}>
                {type.replace('_', ' ')}
            </Badge>
        );
    };

    if (isLoading && houses.length === 0) {
        return (
            <div className="flex items-center justify-center min-h-[400px]">
                <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
        );
    }

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-3xl font-bold text-text">Poultry Houses</h1>
                    <p className="text-text-muted mt-1">Manage your poultry house facilities</p>
                </div>
                <Button onClick={openCreateDialog} className="gap-2">
                    <Plus className="h-5 w-5" />
                    New House
                </Button>
            </div>

            {/* Error Alert */}
            {error && (
                <Alert variant="destructive">
                    <AlertCircle className="h-4 w-4" />
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
            )}

            {/* Stats Cards */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <Card className="p-6">
                    <div className="flex items-center gap-3">
                        <div className="p-3 bg-primary/10 rounded-lg">
                            <Building2 className="h-6 w-6 text-primary" />
                        </div>
                        <div>
                            <div className="text-sm font-medium text-text-muted">Total Houses</div>
                            <div className="text-2xl font-bold text-text">{houses.length}</div>
                        </div>
                    </div>
                </Card>

                <Card className="p-6">
                    <div className="text-sm font-medium text-text-muted">Empty</div>
                    <div className="text-2xl font-bold text-green-600 mt-2">
                        {houses.filter(h => h.status === 'EMPTY').length}
                    </div>
                </Card>

                <Card className="p-6">
                    <div className="text-sm font-medium text-text-muted">Occupied</div>
                    <div className="text-2xl font-bold text-blue-600 mt-2">
                        {houses.filter(h => h.status === 'OCCUPIED').length}
                    </div>
                </Card>

                <Card className="p-6">
                    <div className="text-sm font-medium text-text-muted">Total Capacity</div>
                    <div className="text-2xl font-bold text-primary mt-2">
                        {houses.reduce((sum, h) => sum + (h.capacity || 0), 0).toLocaleString()}
                    </div>
                </Card>
            </div>

            {/* Data Table */}
            <Card>
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>ID</TableHead>
                            <TableHead>Type</TableHead>
                            <TableHead>Capacity</TableHead>
                            <TableHead>Area (m²)</TableHead>
                            <TableHead>Status</TableHead>
                            <TableHead>Occupancy</TableHead>
                            <TableHead>Equipment</TableHead>
                            <TableHead>Current Lot</TableHead>
                            <TableHead className="w-[80px]">Actions</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {houses.length === 0 ? (
                            <TableRow>
                                <TableCell colSpan={9} className="text-center py-12 text-text-muted">
                                    No poultry houses found. Create your first house to get started.
                                </TableCell>
                            </TableRow>
                        ) : (
                            houses.map((house) => (
                                <TableRow key={house.id}>
                                    <TableCell className="font-medium">#{house.id}</TableCell>
                                    <TableCell>{getTypeBadge(house.type)}</TableCell>
                                    <TableCell>{house.capacity?.toLocaleString()} birds</TableCell>
                                    <TableCell>{house.area} m²</TableCell>
                                    <TableCell>{getStatusBadge(house.status)}</TableCell>
                                    <TableCell>
                                        <div className="flex items-center gap-2">
                                            <span className="font-medium">{house.currentOccupancy || 0}</span>
                                            <span className="text-text-muted text-sm">
                                                / {house.capacity}
                                            </span>
                                        </div>
                                    </TableCell>
                                    <TableCell className="text-text-muted">
                                        {house.equipmentType || '—'}
                                    </TableCell>
                                    <TableCell className="text-text-muted">
                                        {house.currentLot || '—'}
                                    </TableCell>
                                    <TableCell>
                                        <DropdownMenu>
                                            <DropdownMenuTrigger asChild>
                                                <Button variant="ghost" size="icon">
                                                    <MoreVertical className="h-4 w-4" />
                                                </Button>
                                            </DropdownMenuTrigger>
                                            <DropdownMenuContent align="end">
                                                <DropdownMenuItem
                                                    onClick={() => openEditDialog(house)}
                                                    className="gap-2"
                                                >
                                                    <Edit className="h-4 w-4" />
                                                    Edit
                                                </DropdownMenuItem>
                                                {house.status === 'EMPTY' && (
                                                    <DropdownMenuItem
                                                        onClick={() => openDeleteDialog(house.id!)}
                                                        className="gap-2 text-red-600 focus:text-red-600 focus:bg-red-50 dark:focus:bg-red-950"
                                                    >
                                                        <Trash2 className="h-4 w-4" />
                                                        Delete
                                                    </DropdownMenuItem>
                                                )}
                                            </DropdownMenuContent>
                                        </DropdownMenu>
                                    </TableCell>
                                </TableRow>
                            ))
                        )}
                    </TableBody>
                </Table>
            </Card>

            {/* Create/Edit Dialog */}
            <Dialog open={formDialogOpen} onOpenChange={setFormDialogOpen}>
                <DialogContent className="max-w-2xl bg-card">
                    <DialogHeader>
                        <DialogTitle>
                            {editingHouse ? 'Edit Poultry House' : 'New Poultry House'}
                        </DialogTitle>
                        <DialogDescription>
                            {editingHouse
                                ? 'Update the details of this poultry house'
                                : 'Add a new poultry house to your farm'}
                        </DialogDescription>
                    </DialogHeader>

                    <div className="grid grid-cols-2 gap-4 py-4">
                        {/* Farm (auto-filled, read-only) */}
                        <div className="space-y-2">
                            <Label>Farm</Label>
                            <Input
                                value={`Farm #${formData.farmId}`}
                                disabled
                                className="bg-neutral-100 dark:bg-neutral-800"
                            />
                        </div>

                        {/* Type */}
                        <div className="space-y-2">
                            <Label htmlFor="type">House Type *</Label>
                            <Select
                                value={formData.type}
                                onValueChange={(value) => setFormData({ ...formData, type: value as PoultryHouseType })}
                            >
                                <SelectTrigger>
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="OPEN">Open</SelectItem>
                                    <SelectItem value="CLOSED">Closed</SelectItem>
                                    <SelectItem value="SEMI_CLOSED">Semi-Closed</SelectItem>
                                </SelectContent>
                            </Select>
                        </div>

                        {/* Capacity */}
                        <div className="space-y-2">
                            <Label htmlFor="capacity">Capacity (birds) *</Label>
                            <Input
                                id="capacity"
                                type="number"
                                value={formData.capacity || ''}
                                onChange={(e) => setFormData({ ...formData, capacity: parseInt(e.target.value) || 0 })}
                                placeholder="e.g., 5000"
                            />
                        </div>

                        {/* Area */}
                        <div className="space-y-2">
                            <Label htmlFor="area">Area (m²) *</Label>
                            <Input
                                id="area"
                                type="number"
                                step="0.01"
                                value={formData.area || ''}
                                onChange={(e) => setFormData({ ...formData, area: parseFloat(e.target.value) || 0 })}
                                placeholder="e.g., 150.5"
                            />
                        </div>

                        {/* Equipment Type */}
                        <div className="space-y-2">
                            <Label htmlFor="equipmentType">Equipment Type</Label>
                            <Input
                                id="equipmentType"
                                value={formData.equipmentType || ''}
                                onChange={(e) => setFormData({ ...formData, equipmentType: e.target.value })}
                                placeholder="e.g., Automatic feeders"
                            />
                        </div>

                        {/* Status */}
                        <div className="space-y-2">
                            <Label htmlFor="status">Status</Label>
                            <Select
                                value={formData.status}
                                onValueChange={(value) => setFormData({ ...formData, status: value as PoultryHouseStatus })}
                            >
                                <SelectTrigger>
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="EMPTY">Empty</SelectItem>
                                    <SelectItem value="OCCUPIED">Occupied</SelectItem>
                                    <SelectItem value="MAINTENANCE">Maintenance</SelectItem>
                                </SelectContent>
                            </Select>
                        </div>

                        {/* Current Occupancy (only show for edit or OCCUPIED) */}
                        {(editingHouse || formData.status === 'OCCUPIED') && (
                            <div className="space-y-2">
                                <Label htmlFor="currentOccupancy">Current Occupancy</Label>
                                <Input
                                    id="currentOccupancy"
                                    type="number"
                                    value={formData.currentOccupancy || ''}
                                    onChange={(e) => setFormData({ ...formData, currentOccupancy: parseInt(e.target.value) || 0 })}
                                    placeholder="0"
                                />
                            </div>
                        )}
                    </div>

                    <DialogFooter>
                        <Button variant="outline" onClick={() => setFormDialogOpen(false)}>
                            Cancel
                        </Button>
                        <Button onClick={handleSubmit}>
                            {editingHouse ? 'Update' : 'Create'} House
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            {/* Delete Confirmation Dialog */}
            <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
                <AlertDialogContent className="bg-card">
                    <AlertDialogHeader>
                        <AlertDialogTitle>Are you sure?</AlertDialogTitle>
                        <AlertDialogDescription>
                            This will permanently delete the poultry house. This action cannot be undone.
                            Only empty houses can be deleted.
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>Cancel</AlertDialogCancel>
                        <AlertDialogAction
                            onClick={handleDelete}
                            className="bg-red-600 hover:bg-red-700 text-white"
                        >
                            Delete
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </div>
    );
}