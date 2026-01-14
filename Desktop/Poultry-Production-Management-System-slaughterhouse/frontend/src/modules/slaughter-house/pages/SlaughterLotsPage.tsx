// src/modules/slaughter-house/pages/SlaughterLotsPage.tsx
import { useEffect, useState } from 'react';
import { useSlaughterLotsStore } from '../stores/useSlaughterLotsStore';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
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

export default function SlaughterLotsPage() {
    const { 
        slaughterLots, 
        isLoading, 
        error,
        fetchSlaughterLots, 
        createSlaughterLot,
        updateSlaughterLot,
        deleteSlaughterLot 
    } = useSlaughterLotsStore();

    const [dialogOpen, setDialogOpen] = useState(false);
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
    const [selectedLot, setSelectedLot] = useState<any>(null);
    const [isEditMode, setIsEditMode] = useState(false);

    const [formData, setFormData] = useState({
        lot_number: '',
        breed: '',
        total_chickens: '',
        slaughter_date: '',
        average_weight_value: '',
        status: 'RECEIVED',
    });

    useEffect(() => {
        fetchSlaughterLots();
    }, [fetchSlaughterLots]);

    const handleOpenDialog = (lot?: any) => {
        if (lot) {
            setIsEditMode(true);
            setSelectedLot(lot);
            setFormData({
                lot_number: lot.lot_number || '',
                breed: lot.breed || '',
                total_chickens: lot.total_chickens?.toString() || lot.current_quantity?.toString() || '',
                slaughter_date: lot.slaughter_date || '',
                average_weight_value: lot.average_weight_value?.toString() || '',
                status: lot.status || 'RECEIVED',
            });
        } else {
            setIsEditMode(false);
            setSelectedLot(null);
            setFormData({
                lot_number: '',
                breed: '',
                total_chickens: '',
                slaughter_date: '',
                average_weight_value: '',
                status: 'RECEIVED',
            });
        }
        setDialogOpen(true);
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const totalChickens = parseInt(formData.total_chickens);

            const data = {
                lot_number: formData.lot_number,
                breed: formData.breed,
                total_chickens: totalChickens,
                current_quantity: totalChickens, // ⭐ Same as total initially
                slaughter_date: formData.slaughter_date || null,
                average_weight_value: parseFloat(formData.average_weight_value),
                average_weight_unit: 'kg', // ⭐ Default unit
                status: formData.status,
                is_active: true,
            };

            console.log('📤 Sending SlaughterLot data:', data);

            if (isEditMode && selectedLot) {
                await updateSlaughterLot(selectedLot.id, data);
            } else {
                await createSlaughterLot(data);
            }
            setDialogOpen(false);
            fetchSlaughterLots();
        } catch (error) {
            console.error('Failed to save slaughter lot:', error);
        }
    };

    const handleDelete = async () => {
        if (selectedLot) {
            try {
                await deleteSlaughterLot(selectedLot.id);
                setDeleteDialogOpen(false);
                setSelectedLot(null);
            } catch (error) {
                console.error('Delete failed:', error);
            }
        }
    };

    const openDeleteDialog = (lot: any) => {
        setSelectedLot(lot);
        setDeleteDialogOpen(true);
    };

    const getStatusBadge = (status: string) => {
        const variants: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
            RECEIVED: 'default',
            AWAITING_INSPECTION: 'outline',
            INSPECTION_APPROVED: 'default',
            INSPECTION_REJECTED: 'destructive',
            IN_PROCESSING: 'secondary',
            PROCESSING_COMPLETE: 'default',
            PACKAGED: 'default',
            PENDING: 'outline',
        };
        return (
            <Badge variant={variants[status] || 'default'}>
                {status.replace(/_/g, ' ')}
            </Badge>
        );
    };

    if (isLoading && slaughterLots.length === 0) {
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
                    <h1 className="text-3xl font-bold text-text">Slaughter Lots</h1>
                    <p className="text-text-muted mt-1">Manage slaughter lot records</p>
                </div>
                <Button
                    onClick={() => handleOpenDialog()}
                    className="gap-2"
                >
                    <Plus className="h-5 w-5" />
                    New Slaughter Lot
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
                    <p className="text-sm text-text-muted">Total Lots</p>
                    <p className="text-2xl font-bold">{slaughterLots.length}</p>
                </Card>
                <Card className="p-4">
                    <p className="text-sm text-text-muted">Total Chickens</p>
                    <p className="text-2xl font-bold">
                        {slaughterLots.reduce((sum, lot) => sum + (lot.total_chickens || lot.current_quantity || 0), 0)}
                    </p>
                </Card>
                <Card className="p-4">
                    <p className="text-sm text-text-muted">In Process</p>
                    <p className="text-2xl font-bold">
                        {slaughterLots.filter(lot => lot.status === 'IN_PROCESSING').length}
                    </p>
                </Card>
            </div>

            {/* Slaughter Lots Table */}
            <Card>
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>ID</TableHead>
                            <TableHead>Lot Number</TableHead>
                            <TableHead>Breed</TableHead>
                            <TableHead>Quantity</TableHead>
                            <TableHead>Avg Weight</TableHead>
                            <TableHead>Source Farm</TableHead>
                            <TableHead>Status</TableHead>
                            <TableHead>Arrival Date</TableHead>
                            <TableHead>Actions</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {slaughterLots.length === 0 ? (
                            <TableRow>
                                <TableCell colSpan={9} className="text-center py-8 text-text-muted">
                                    No slaughter lots found. Create your first lot!
                                </TableCell>
                            </TableRow>
                        ) : (
                            slaughterLots.map((lot) => (
                                <TableRow key={lot.id}>
                                    <TableCell>{lot.id}</TableCell>
                                    <TableCell className="font-medium">{lot.lot_number}</TableCell>
                                    <TableCell>{lot.breed || 'N/A'}</TableCell>
                                    <TableCell>{lot.current_quantity || lot.total_chickens || 0}</TableCell>
                                    <TableCell>
                                        {lot.average_weight_value
                                            ? `${lot.average_weight_value} ${lot.average_weight_unit || 'kg'}`
                                            : 'N/A'}
                                    </TableCell>
                                    <TableCell>N/A</TableCell>
                                    <TableCell>{getStatusBadge(lot.status || 'PENDING')}</TableCell>
                                    <TableCell>
                                        {lot.slaughter_date
                                            ? format(new Date(lot.slaughter_date), 'PP')
                                            : 'N/A'}
                                    </TableCell>
                                    <TableCell>
                                        <div className="flex gap-2">
                                            <Button
                                                variant="outline"
                                                size="sm"
                                                onClick={() => handleOpenDialog(lot)}
                                            >
                                                <Edit className="h-4 w-4" />
                                            </Button>
                                            <Button
                                                variant="destructive"
                                                size="sm"
                                                onClick={() => openDeleteDialog(lot)}
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
                <DialogContent className="sm:max-w-[600px] bg-gray-900 border-2 border-gray-700">
                    <DialogHeader>
                        <DialogTitle className="text-xl font-bold">
                            {isEditMode ? 'Edit Slaughter Lot' : 'Create New Slaughter Lot'}
                        </DialogTitle>
                    </DialogHeader>
                    <form onSubmit={handleSubmit} className="space-y-4">
                        {/* Lot Number */}
                        <div className="space-y-2">
                            <Label htmlFor="lot_number">Lot Number *</Label>
                            <Input
                                id="lot_number"
                                value={formData.lot_number}
                                onChange={(e) => setFormData({ ...formData, lot_number: e.target.value })}
                                className="bg-gray-800 border-gray-700"
                                placeholder="e.g., LOT-2026"
                                required
                            />
                        </div>

                        {/* Breed */}
                        <div className="space-y-2">
                            <Label htmlFor="breed">Breed *</Label>
                            <select
                                id="breed"
                                value={formData.breed}
                                onChange={(e) => setFormData({ ...formData, breed: e.target.value })}
                                className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-md text-white focus:outline-none focus:ring-2 focus:ring-red-500"
                                required
                            >
                                <option value="">Select Breed</option>
                                <option value="Ross 308">Ross 308</option>
                                <option value="Ross 309">Ross 309</option>
                                <option value="Ross 710">Ross 710</option>
                                <option value="Cobb 500">Cobb 500</option>
                                <option value="Arbor Acres">Arbor Acres</option>
                                <option value="Hubbard">Hubbard</option>
                                <option value="Other">Other</option>
                            </select>
                        </div>

                        {/* Total Chickens */}
                        <div className="space-y-2">
                            <Label htmlFor="total_chickens">Total Chickens *</Label>
                            <Input
                                id="total_chickens"
                                type="number"
                                value={formData.total_chickens}
                                onChange={(e) => setFormData({ ...formData, total_chickens: e.target.value })}
                                className="bg-gray-800 border-gray-700"
                                placeholder="e.g., 1000"
                                required
                            />
                        </div>

                        {/* Slaughter Date */}
                        <div className="space-y-2">
                            <Label htmlFor="slaughter_date">Slaughter Date</Label>
                            <Input
                                id="slaughter_date"
                                type="date"
                                value={formData.slaughter_date}
                                onChange={(e) => setFormData({ ...formData, slaughter_date: e.target.value })}
                                className="bg-gray-800 border-gray-700"
                            />
                        </div>

                        {/* Average Weight */}
                        <div className="space-y-2">
                            <Label htmlFor="average_weight_value">Average Weight (kg) *</Label>
                            <Input
                                id="average_weight_value"
                                type="number"
                                step="0.01"
                                value={formData.average_weight_value}
                                onChange={(e) => setFormData({ ...formData, average_weight_value: e.target.value })}
                                className="bg-gray-800 border-gray-700"
                                placeholder="e.g., 2.5"
                                required
                            />
                        </div>

                        {/* Status */}
                        <div className="space-y-2">
                            <Label htmlFor="status">Status *</Label>
                            <select
                                id="status"
                                value={formData.status}
                                onChange={(e) => setFormData({ ...formData, status: e.target.value })}
                                className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-md text-white focus:outline-none focus:ring-2 focus:ring-red-500"
                                required
                            >
                                <option value="RECEIVED">Received</option>
                                <option value="AWAITING_INSPECTION">Awaiting Inspection</option>
                                <option value="INSPECTION_APPROVED">Inspection Approved</option>
                                <option value="INSPECTION_REJECTED">Inspection Rejected</option>
                                <option value="IN_PROCESSING">In Processing</option>
                                <option value="PROCESSING_COMPLETE">Processing Complete</option>
                                <option value="PACKAGED">Packaged</option>
                                <option value="IN_STORAGE">In Storage</option>
                                <option value="READY_FOR_DELIVERY">Ready for Delivery</option>
                                <option value="DELIVERED">Delivered</option>
                                <option value="PENDING">Pending</option>
                                <option value="CLOSED">Closed</option>
                            </select>
                        </div>

                        {/* Form Actions */}
                        <div className="flex justify-end gap-2 pt-4">
                            <Button type="button" variant="outline" onClick={() => setDialogOpen(false)}>
                                Cancel
                            </Button>
                            <Button type="submit" className="bg-red-600 hover:bg-red-700">
                                {isEditMode ? 'Update' : 'Create'}
                            </Button>
                        </div>
                    </form>
                </DialogContent>
            </Dialog>

            {/* Delete Confirmation Dialog */}
            <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
                <AlertDialogContent className="bg-gray-900 border-2 border-gray-700">
                    <AlertDialogHeader>
                        <AlertDialogTitle>Are you sure?</AlertDialogTitle>
                        <AlertDialogDescription>
                            This will permanently delete the slaughter lot. This action cannot be undone.
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>Cancel</AlertDialogCancel>
                        <AlertDialogAction onClick={handleDelete} className="bg-red-600 hover:bg-red-700">
                            Delete
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </div>
    );
}