// src/pages/ChicksReceptionsPage.tsx
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { useReceptionStore } from '@/modules/broiler-farm/stores/useReceptionStore';
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
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
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
import { Plus, MoreVertical, Eye, Edit, Trash2, Loader2 } from 'lucide-react';
import { format } from 'date-fns';

export default function ChicksReceptionsPage() {
    const navigate = useNavigate();
    const { receptions, isLoading, fetchReceptions, deleteReception } = useReceptionStore();
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
    const [selectedReception, setSelectedReception] = useState<number | null>(null);

    useEffect(() => {
        fetchReceptions();
    }, [fetchReceptions]);

    const handleDelete = async () => {
        if (selectedReception) {
            try {
                await deleteReception(selectedReception);
                setDeleteDialogOpen(false);
                setSelectedReception(null);
            } catch (error) {
                console.error('Delete failed:', error);
            }
        }
    };

    const openDeleteDialog = (id: number) => {
        setSelectedReception(id);
        setDeleteDialogOpen(true);
    };

    const getStatusBadge = (status: string) => {
        const variants: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
            DRAFT: 'outline',
            FINALIZED: 'default',
        };
        return (
            <Badge variant={variants[status] || 'default'}>
                {status}
            </Badge>
        );
    };

    if (isLoading) {
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
                    <h1 className="text-3xl font-bold text-text">Chicks Receptions</h1>
                    <p className="text-text-muted mt-1">Manage chick reception records</p>
                </div>
                <Button
                    onClick={() => navigate('/chicks-receptions/create')}
                    className="gap-2 text-neutral-50"
                >
                    <Plus className="h-5 w-5 " />
                    New Reception
                </Button>
            </div>

            {/* Stats Cards */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <Card className="p-6">
                    <div className="text-sm font-medium text-text-muted">Total Receptions</div>
                    <div className="text-3xl font-bold text-text mt-2">{receptions.length}</div>
                </Card>
                <Card className="p-6">
                    <div className="text-sm font-medium text-text-muted">Draft</div>
                    <div className="text-3xl font-bold text-yellow-600 mt-2">
                        {receptions.filter(r => r.receptionStatus === 'DRAFT').length}
                    </div>
                </Card>
                <Card className="p-6">
                    <div className="text-sm font-medium text-text-muted">Finalized</div>
                    <div className="text-3xl font-bold text-green-600 mt-2">
                        {receptions.filter(r => r.receptionStatus === 'CONFIRMED').length}
                    </div>
                </Card>
            </div>

            {/* Data Table */}
            <Card>
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>ID</TableHead>
                            <TableHead>Reception Date</TableHead>
                            <TableHead>Status</TableHead>
                            <TableHead>Lines</TableHead>
                            <TableHead>Total Chicks</TableHead>
                            <TableHead>Reference Doc</TableHead>
                            <TableHead className="w-[80px]">Actions</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {receptions.length === 0 ? (
                            <TableRow>
                                <TableCell colSpan={7} className="text-center py-12 text-text-muted">
                                    No receptions found. Create your first reception to get started.
                                </TableCell>
                            </TableRow>
                        ) : (
                            receptions.map((reception) => {
                                // console.log(reception)
                                const totalChicks = reception.lines?.reduce(
                                    (sum, line) => sum + (line.chicksAlive || 0),
                                    0
                                ) || 0;

                                return (
                                    <TableRow key={reception.id}>
                                        <TableCell className="font-medium text-neutral-50">#{reception.id}</TableCell>
                                        <TableCell>
                                            {reception.receptionDate
                                                ? format(new Date(reception.receptionDate), 'MMM dd, yyyy HH:mm')
                                                : 'N/A'}
                                        </TableCell>
                                        <TableCell className='text-neutral-50'>{getStatusBadge(reception.receptionStatus || 'DRAFT')}</TableCell>
                                        <TableCell>{reception.lines?.length || 0}</TableCell>
                                        <TableCell className="font-semibold">{totalChicks.toLocaleString()}</TableCell>
                                        <TableCell className="text-text-muted">
                                            {reception.referenceDocument || '—'}
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
                                                        onClick={() => navigate(`/chicks-receptions/${reception.id}`)}
                                                        className="gap-2"
                                                    >
                                                        <Eye className="h-4 w-4" />
                                                        View Details
                                                    </DropdownMenuItem>
                                                    {reception.receptionStatus === 'DRAFT' && (
                                                        <DropdownMenuItem
                                                            onClick={() => navigate(`/chicks-receptions/${reception.id}/edit`)}
                                                            className="gap-2"
                                                        >
                                                            <Edit className="h-4 w-4" />
                                                            Edit
                                                        </DropdownMenuItem>
                                                    )}
                                                    {reception.receptionStatus && (
                                                        <DropdownMenuItem
                                                            onClick={() => openDeleteDialog(reception.id!)}
                                                            className="gap-2 text-red-600 focus:text-red-600"
                                                        >
                                                            <Trash2 className="h-4 w-4" />
                                                            Delete
                                                        </DropdownMenuItem>
                                                    )}
                                                </DropdownMenuContent>
                                            </DropdownMenu>
                                        </TableCell>
                                    </TableRow>
                                );
                            })
                        )}
                    </TableBody>
                </Table>
            </Card>

            {/* Delete Confirmation Dialog */}
            <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Are you sure?</AlertDialogTitle>
                        <AlertDialogDescription>
                            This will permanently delete the reception. This action cannot be undone.
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>Cancel</AlertDialogCancel>
                        <AlertDialogAction
                            onClick={handleDelete}
                            className="bg-red-600 hover:bg-red-700"
                        >
                            Delete
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </div>
    );
}