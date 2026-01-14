// src/modules/slaughter-house/pages/ProductsPage.tsx
import { useEffect, useState } from 'react';
import { useProductsStore } from '../stores/useProductsStore';
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
import { Plus, Trash2, Edit, Loader2, CheckCircle, XCircle } from 'lucide-react';
import { format } from 'date-fns';

export default function ProductsPage() {
    const { 
        products, 
        isLoading, 
        error,
        fetchProducts, 
        createProduct,
        updateProduct,
        deleteProduct,
        inspectProduct 
    } = useProductsStore();

    const [dialogOpen, setDialogOpen] = useState(false);
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
    const [selectedProduct, setSelectedProduct] = useState<any>(null);
    const [isEditMode, setIsEditMode] = useState(false);

    // Form state
    const [formData, setFormData] = useState({
        product_type: '',
        slaughter_lot_id: '',
        weight_value: '',
        quality_grade: '',
        status: 'PRODUCED', // ✅ Changed from 'RAW' to 'PRODUCED'
        batch_number: '',
    });

    useEffect(() => {
        fetchProducts();
    }, [fetchProducts]);

    const handleOpenDialog = (product?: any) => {
        if (product) {
            setIsEditMode(true);
            setSelectedProduct(product);
            setFormData({
                product_type: product.product_type || '',
                slaughter_lot_id: product.slaughter_lot_id?.toString() || '',
                weight_value: product.weight_value?.toString() || '',
                quality_grade: product.quality_grade || '',
                status: product.status || 'PRODUCED', // ✅ Changed
                batch_number: product.batch_number || '',
            });
        } else {
            setIsEditMode(false);
            setSelectedProduct(null);
            setFormData({
                product_type: '',
                slaughter_lot_id: '',
                weight_value: '',
                quality_grade: '',
                status: 'PRODUCED', // ✅ Changed
                batch_number: '',
            });
        }
        setDialogOpen(true);
    };

const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
        const data = {
            product_type: formData.product_type,           // ✅ Enum value
            slaughter_lot_id: parseInt(formData.slaughter_lot_id),
            weight_value: parseFloat(formData.weight_value),
            weight_unit: 'kg',
            quality_grade: formData.quality_grade || null,
            status: formData.status,                        // ✅ Enum value
            batch_number: formData.batch_number || null,
            is_active: true,
        };

        console.log('📤 Sending data:', data);

        if (isEditMode && selectedProduct) {
            await updateProduct(selectedProduct.id, data);
        } else {
            await createProduct(data);
        }
        setDialogOpen(false);
        fetchProducts();
    } catch (error: any) {
        console.error('❌ Failed to save product:', error);
        console.error('❌ Error response:', error.response?.data);
    }
};

    const handleDelete = async () => {
        if (selectedProduct) {
            try {
                await deleteProduct(selectedProduct.id);
                setDeleteDialogOpen(false);
                setSelectedProduct(null);
            } catch (error) {
                console.error('Delete failed:', error);
            }
        }
    };

    const handleInspect = async (id: number, passed: boolean) => {
        try {
            await inspectProduct(id, passed);
        } catch (error) {
            console.error('Inspection failed:', error);
        }
    };

    const openDeleteDialog = (product: any) => {
        setSelectedProduct(product);
        setDeleteDialogOpen(true);
    };

    const getStatusBadge = (status: string) => {
        const variants: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
            PRODUCED: 'outline',
            INSPECTED: 'secondary',
            APPROVED: 'default',
            REJECTED: 'destructive',
            PACKAGED: 'default',
            IN_STORAGE: 'secondary',
            READY_FOR_DELIVERY: 'default',
            DELIVERED: 'default',
        };
        return (
            <Badge variant={variants[status] || 'default'}>
                {status.replace(/_/g, ' ')}
            </Badge>
        );
    };

    if (isLoading && products.length === 0) {
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
                    <h1 className="text-3xl font-bold text-text">Products</h1>
                    <p className="text-text-muted mt-1">Manage slaughterhouse products</p>
                </div>
                <Button
                    onClick={() => handleOpenDialog()}
                    className="gap-2"
                >
                    <Plus className="h-5 w-5" />
                    New Product
                </Button>
            </div>

            {/* Error Display */}
            {error && (
                <div className="bg-red-500/10 border border-red-500 text-red-500 px-4 py-3 rounded">
                    {error}
                </div>
            )}

            {/* Stats Cards */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <Card className="p-4">
                    <p className="text-sm text-text-muted">Total Products</p>
                    <p className="text-2xl font-bold">{products.length}</p>
                </Card>
                <Card className="p-4">
                    <p className="text-sm text-text-muted">Produced</p>
                    <p className="text-2xl font-bold">
                        {products.filter(p => p.status === 'PRODUCED').length}
                    </p>
                </Card>
                <Card className="p-4">
                    <p className="text-sm text-text-muted">Inspected</p>
                    <p className="text-2xl font-bold">
                        {products.filter(p => p.status === 'INSPECTED').length}
                    </p>
                </Card>
                <Card className="p-4">
                    <p className="text-sm text-text-muted">Packaged</p>
                    <p className="text-2xl font-bold">
                        {products.filter(p => p.status === 'PACKAGED').length}
                    </p>
                </Card>
            </div>

            {/* Products Table */}
            <Card>
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>ID</TableHead>
                            <TableHead>Type</TableHead>
                            <TableHead>Lot ID</TableHead>
                            <TableHead>Weight</TableHead>
                            <TableHead>Quality</TableHead>
                            <TableHead>Status</TableHead>
                            <TableHead>Batch</TableHead>
                            <TableHead>Date</TableHead>
                            <TableHead>Actions</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {products.length === 0 ? (
                            <TableRow>
                                <TableCell colSpan={9} className="text-center py-8 text-text-muted">
                                    No products found. Create your first product!
                                </TableCell>
                            </TableRow>
                        ) : (
                            products.map((product) => (
                                <TableRow key={product.id}>
                                    <TableCell>{product.id}</TableCell>
                                    <TableCell className="font-medium">{product.product_type}</TableCell>
                                    <TableCell>{product.slaughter_lot_id}</TableCell>
                                    <TableCell>{product.weight_value} kg</TableCell>
                                    <TableCell>{product.quality_grade || 'N/A'}</TableCell>
                                    <TableCell>{getStatusBadge(product.status)}</TableCell>
                                    <TableCell>{product.batch_number || 'N/A'}</TableCell>
                                    <TableCell>
                                        {product.production_date
                                            ? format(new Date(product.production_date), 'PP')
                                            : 'N/A'}
                                    </TableCell>
                                    <TableCell>
                                        <div className="flex gap-2">
                                            <Button
                                                variant="outline"
                                                size="sm"
                                                onClick={() => handleOpenDialog(product)}
                                            >
                                                <Edit className="h-4 w-4" />
                                            </Button>
                                            {product.status === 'PRODUCED' && (
                                                <>
                                                    <Button
                                                        variant="outline"
                                                        size="sm"
                                                        onClick={() => handleInspect(product.id!, true)}
                                                        title="Pass Inspection"
                                                    >
                                                        <CheckCircle className="h-4 w-4 text-green-500" />
                                                    </Button>
                                                    <Button
                                                        variant="outline"
                                                        size="sm"
                                                        onClick={() => handleInspect(product.id!, false)}
                                                        title="Fail Inspection"
                                                    >
                                                        <XCircle className="h-4 w-4 text-red-500" />
                                                    </Button>
                                                </>
                                            )}
                                            <Button
                                                variant="destructive"
                                                size="sm"
                                                onClick={() => openDeleteDialog(product)}
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
                            {isEditMode ? 'Edit Product' : 'Create New Product'}
                        </DialogTitle>
                    </DialogHeader>
                    <form onSubmit={handleSubmit} className="space-y-4">
                        {/* Product Type Dropdown */}
                        {/* Product Type Dropdown */}
                        <div className="space-y-2">
                            <Label htmlFor="product_type">Product Type *</Label>
                            <select
                                id="product_type"
                                value={formData.product_type}
                                onChange={(e) => setFormData({ ...formData, product_type: e.target.value })}
                                className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-md text-white focus:outline-none focus:ring-2 focus:ring-red-500"
                                required
                            >
                                <option value="">Select Type</option>
                                <option value="WHOLE_CHICKEN">Whole Chicken</option>
                                <option value="BREAST">Chicken Breast</option>
                                <option value="THIGH">Chicken Thigh</option>
                                <option value="DRUMSTICK">Chicken Drumstick</option>
                                <option value="WING">Chicken Wing</option>
                                <option value="LIVER">Chicken Liver</option>
                                <option value="HEART">Chicken Heart</option>
                                <option value="GIZZARD">Chicken Gizzard</option>
                                <option value="FEET">Chicken Feet</option>
                                <option value="MINCED">Minced Chicken</option>
                                <option value="OTHER">Other Product</option>
                            </select>
                        </div>

                        {/* Slaughter Lot ID */}
                        <div className="space-y-2">
                            <Label htmlFor="slaughter_lot_id">Slaughter Lot ID *</Label>
                            <Input
                                id="slaughter_lot_id"
                                type="number"
                                value={formData.slaughter_lot_id}
                                onChange={(e) => setFormData({ ...formData, slaughter_lot_id: e.target.value })}
                                className="bg-gray-800 border-gray-700"
                                required
                            />
                        </div>

                        {/* Weight */}
                        <div className="space-y-2">
                            <Label htmlFor="weight_value">Weight (kg) *</Label>
                            <Input
                                id="weight_value"
                                type="number"
                                step="0.01"
                                value={formData.weight_value}
                                onChange={(e) => setFormData({ ...formData, weight_value: e.target.value })}
                                className="bg-gray-800 border-gray-700"
                                required
                            />
                        </div>

                        {/* Status Dropdown */}
                        <div className="space-y-2">
                            <Label htmlFor="status">Status *</Label>
                            <select
                                id="status"
                                value={formData.status}
                                onChange={(e) => setFormData({ ...formData, status: e.target.value })}
                                className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-md text-white focus:outline-none focus:ring-2 focus:ring-red-500"
                                required
                            >
                                <option value="PRODUCED">Produced</option>
                                <option value="INSPECTED">Inspected</option>
                                <option value="APPROVED">Approved for packaging</option>
                                <option value="REJECTED">Rejected</option>
                                <option value="PACKAGED">Packaged</option>
                                <option value="IN_STORAGE">In cold storage</option>
                                <option value="READY_FOR_DELIVERY">Ready for delivery</option>
                                <option value="DELIVERED">Delivered</option>
                            </select>
                        </div>

                        {/* Quality Grade */}
                        <div className="space-y-2">
                            <Label htmlFor="quality_grade">Quality Grade</Label>
                            <select
                                id="quality_grade"
                                value={formData.quality_grade}
                                onChange={(e) => setFormData({ ...formData, quality_grade: e.target.value })}
                                className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-md text-white focus:outline-none focus:ring-2 focus:ring-red-500"
                            >
                                <option value="">Select Grade</option>
                                <option value="A">Grade A</option>
                                <option value="B">Grade B</option>
                                <option value="C">Grade C</option>
                            </select>
                        </div>

                        {/* Batch Number */}
                        <div className="space-y-2">
                            <Label htmlFor="batch_number">Batch Number</Label>
                            <Input
                                id="batch_number"
                                value={formData.batch_number}
                                onChange={(e) => setFormData({ ...formData, batch_number: e.target.value })}
                                className="bg-gray-800 border-gray-700"
                                placeholder="e.g., BATCH-001"
                            />
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
                            This will permanently delete the product. This action cannot be undone.
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