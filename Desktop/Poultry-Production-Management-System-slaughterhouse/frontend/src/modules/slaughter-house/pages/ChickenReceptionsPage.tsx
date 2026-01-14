// src/modules/slaughter-house/pages/ChickenReceptionsPage.tsx
import { useEffect, useState } from 'react';
import { useChickenReceptionsStore } from '../stores/useChickenReceptionsStore';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
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
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Plus, Trash2, Edit, Loader2 } from 'lucide-react';
import { format } from 'date-fns';

export default function ChickenReceptionsPage() {
    const { 
        receptions, 
        isLoading, 
        error,
        fetchReceptions, 
        createReception,
        updateReception,
        deleteReception 
    } = useChickenReceptionsStore();

    const [dialogOpen, setDialogOpen] = useState(false);
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
    const [selectedReception, setSelectedReception] = useState<any>(null);
    const [isEditMode, setIsEditMode] = useState(false);

    const [formData, setFormData] = useState({
        slaughter_lot_id: '',
        quantity_received: '',
        average_weight: '',
        vehicle_info: '',
        driver_name: '',
        temperature_on_arrival: '',
        quality_assessment: '',
        received_by: '',
        reception_notes: '',
    });

    useEffect(() => {
        fetchReceptions();
    }, [fetchReceptions]);

    const handleOpenDialog = (reception?: any) => {
        if (reception) {
            setIsEditMode(true);
            setSelectedReception(reception);
            setFormData({
                slaughter_lot_id: reception.slaughter_lot_id?.toString() || '',
                quantity_received: reception.quantity_received?.toString() || '',
                average_weight: reception.average_weight?.toString() || '',
                vehicle_info: reception.vehicle_info || '',
                driver_name: reception.driver_name || '',
                temperature_on_arrival: reception.temperature_on_arrival?.toString() || '',
                quality_assessment: reception.quality_assessment || '',
                received_by: reception.received_by || '',
                reception_notes: reception.reception_notes || '',
            });
        } else {
            setIsEditMode(false);
            setSelectedReception(null);
            setFormData({
                slaughter_lot_id: '',
                quantity_received: '',
                average_weight: '',
                vehicle_info: '',
                driver_name: '',
                temperature_on_arrival: '',
                quality_assessment: '',
                received_by: '',
                reception_notes: '',
            });
        }
        setDialogOpen(true);
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const data = {
                ...formData,
                slaughter_lot_id: parseInt(formData.slaughter_lot_id),
                quantity_received: parseInt(formData.quantity_received),
                average_weight: parseFloat(formData.average_weight),
                temperature_on_arrival: formData.temperature_on_arrival 
                    ? parseFloat(formData.temperature_on_arrival) 
                    : undefined,
                is_active: true,
            };

            if (isEditMode && selectedReception) {
                await updateReception(selectedReception.id, data);
            } else {
                await createReception(data);
            }
            setDialogOpen(false);
            fetchReceptions();
        } catch (error) {
            console.error('Failed to save reception:', error);
        }
    };

    const handleDelete = async () => {
        if (selectedReception) {
            try {
                await deleteReception(selectedReception.id);
                setDeleteDialogOpen(false);
                setSelectedReception(null);
            } catch (error) {
                console.error('Delete failed:', error);
            }
        }
    };

    const openDeleteDialog = (reception: any) => {
        setSelectedReception(reception);
        setDeleteDialogOpen(true);
    };

    if (isLoading && receptions.length === 0) {
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
                    <h1 className="text-3xl font-bold text-text">Chicken Receptions</h1>
                    <p className="text-text-muted mt-1">Manage chicken reception records</p>
                </div>
                <Button
                    onClick={() => handleOpenDialog()}
                    className="gap-2"
                >
                    <Plus className="h-5 w-5" />
                    New Reception
                </Button>
            </div>

            {/* Error Display */}
            {error && (
                <div className="bg-red-500/10 border border-red-500 text-red-500 px-4 py-3 rounded">
                    {error}
                </div>
            )}

            {/* Stats Cards */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <Card className="p-4">
                    <p className="text-sm text-text-muted">Total Receptions</p>
                    <p className="text-2xl font-bold">{receptions.length}</p>
                </Card>
                <Card className="p-4">
                    <p className="text-sm text-text-muted">Total Received</p>
                    <p className="text-2xl font-bold">
                        {receptions.reduce((sum, r) => sum + (r.quantity_received || 0), 0)}
                    </p>
                </Card>
                <Card className="p-4">
                    <p className="text-sm text-text-muted">Avg Weight</p>
                    <p className="text-2xl font-bold">
                        {receptions.length > 0 
                            ? (receptions.reduce((sum, r) => sum + (r.average_weight || 0), 0) / receptions.length).toFixed(2) 
                            : 0} kg
                    </p>
                </Card>
            </div>

            {/* Receptions Table */}
            <Card>
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>ID</TableHead>
                            <TableHead>Lot ID</TableHead>
                            <TableHead>Quantity</TableHead>
                            <TableHead>Avg Weight</TableHead>
                            <TableHead>Vehicle</TableHead>
                            <TableHead>Driver</TableHead>
                            <TableHead>Temperature</TableHead>
                            <TableHead>Received By</TableHead>
                            <TableHead>Date</TableHead>
                            <TableHead>Actions</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {receptions.length === 0 ? (
                            <TableRow>
                                <TableCell colSpan={10} className="text-center py-8 text-text-muted">
                                    No receptions found. Create your first reception!
                                </TableCell>
                            </TableRow>
                        ) : (
                            receptions.map((reception) => (
                                <TableRow key={reception.id}>
                                    <TableCell>{reception.id}</TableCell>
                                    <TableCell>{reception.slaughter_lot_id}</TableCell>
                                    <TableCell className="font-medium">{reception.quantity_received}</TableCell>
                                    <TableCell>{reception.average_weight} kg</TableCell>
                                    <TableCell>{reception.vehicle_info || 'N/A'}</TableCell>
                                    <TableCell>{reception.driver_name || 'N/A'}</TableCell>
                                    <TableCell>{reception.temperature_on_arrival || 'N/A'}°C</TableCell>
                                    <TableCell>{reception.received_by || 'N/A'}</TableCell>
                                    <TableCell>
                                        {reception.reception_date 
                                            ? format(new Date(reception.reception_date), 'PP') 
                                            : 'N/A'}
                                    </TableCell>
                                    <TableCell>
                                        <div className="flex gap-2">
                                            <Button
                                                variant="outline"
                                                size="sm"
                                                onClick={() => handleOpenDialog(reception)}
                                            >
                                                <Edit className="h-4 w-4" />
                                            </Button>
                                            <Button
                                                variant="destructive"
                                                size="sm"
                                                onClick={() => openDeleteDialog(reception)}
                                            >
                                                <Trash2 className="h-4 w-4" />
                                            </Button>
                                        </div>
                                    </TableCell>
                                </TableRow>
                            ))
                        )}
                    </TableBody>
                </Table>
            </Card>

            {/* Create/Edit Dialog */}
            <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
                <DialogContent className="max-h-[90vh] overflow-y-auto">
                    <DialogHeader>
                        <DialogTitle>
                            {isEditMode ? 'Edit Reception' : 'Create New Reception'}
                        </DialogTitle>
                    </DialogHeader>
                    <form onSubmit={handleSubmit} className="space-y-4">
                        <div>
                            <Label htmlFor="slaughter_lot_id">Slaughter Lot ID</Label>
                            <Input
                                id="slaughter_lot_id"
                                type="number"
                                value={formData.slaughter_lot_id}
                                onChange={(e) => setFormData({ ...formData, slaughter_lot_id: e.target.value })}
                                required
                            />
                        </div>
                        <div>
                            <Label htmlFor="quantity_received">Quantity Received</Label>
                            <Input
                                id="quantity_received"
                                type="number"
                                value={formData.quantity_received}
                                onChange={(e) => setFormData({ ...formData, quantity_received: e.target.value })}
                                required
                            />
                        </div>
                        <div>
                            <Label htmlFor="average_weight">Average Weight (kg)</Label>
                            <Input
                                id="average_weight"
                                type="number"
                                step="0.01"
                                value={formData.average_weight}
                                onChange={(e) => setFormData({ ...formData, average_weight: e.target.value })}
                                required
                            />
                        </div>
                        <div>
                            <Label htmlFor="vehicle_info">Vehicle Info</Label>
                            <Input
                                id="vehicle_info"
                                value={formData.vehicle_info}
                                onChange={(e) => setFormData({ ...formData, vehicle_info: e.target.value })}
                            />
                        </div>
                        <div>
                            <Label htmlFor="driver_name">Driver Name</Label>
                            <Input
                                id="driver_name"
                                value={formData.driver_name}
                                onChange={(e) => setFormData({ ...formData, driver_name: e.target.value })}
                            />
                        </div>
                        <div>
                            <Label htmlFor="temperature_on_arrival">Temperature (°C)</Label>
                            <Input
                                id="temperature_on_arrival"
                                type="number"
                                step="0.1"
                                value={formData.temperature_on_arrival}
                                onChange={(e) => setFormData({ ...formData, temperature_on_arrival: e.target.value })}
                            />
                        </div>
                        <div>
                            <Label htmlFor="received_by">Received By</Label>
                            <Input
                                id="received_by"
                                value={formData.received_by}
                                onChange={(e) => setFormData({ ...formData, received_by: e.target.value })}
                            />
                        </div>
                        <div>
                            <Label htmlFor="quality_assessment">Quality Assessment</Label>
                            <Input
                                id="quality_assessment"
                                value={formData.quality_assessment}
                                onChange={(e) => setFormData({ ...formData, quality_assessment: e.target.value })}
                            />
                        </div>
                        <div>
                            <Label htmlFor="reception_notes">Notes</Label>
                            <Input
                                id="reception_notes"
                                value={formData.reception_notes}
                                onChange={(e) => setFormData({ ...formData, reception_notes: e.target.value })}
                            />
                        </div>
                        <div className="flex justify-end gap-2">
                            <Button type="button" variant="outline" onClick={() => setDialogOpen(false)}>
                                Cancel
                            </Button>
                            <Button type="submit">
                                {isEditMode ? 'Update' : 'Create'}
                            </Button>
                        </div>
                    </form>
                </DialogContent>
            </Dialog>

            {/* Delete Confirmation Dialog */}
            <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Are you sure?</AlertDialogTitle>
                        <AlertDialogDescription>
                            This will permanently delete the reception record. This action cannot be undone.
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>Cancel</AlertDialogCancel>
                        <AlertDialogAction onClick={handleDelete}>
                            Delete
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </div>
    );
}
