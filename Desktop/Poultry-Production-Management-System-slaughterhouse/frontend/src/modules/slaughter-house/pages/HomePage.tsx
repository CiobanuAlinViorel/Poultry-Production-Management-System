// src/modules/slaughter-house/pages/HomePage.tsx
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { useProductsStore } from '../stores/useProductsStore';
import { useSlaughterLotsStore } from '../stores/useSlaughterLotsStore';
import { useChickenReceptionsStore } from '../stores/useChickenReceptionsStore';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Package, Truck, ClipboardList, TrendingUp, ArrowRight } from 'lucide-react';

export default function SlaughterHouseHomePage() {
    const navigate = useNavigate();
    const { products, fetchProducts } = useProductsStore();
    const { slaughterLots, fetchSlaughterLots } = useSlaughterLotsStore();
    const { receptions, fetchReceptions } = useChickenReceptionsStore();

    useEffect(() => {
        fetchProducts();
        fetchSlaughterLots();
        fetchReceptions();
    }, []);

    const stats = [
        {
            title: 'Total Products',
            value: products.length,
            icon: Package,
            color: 'text-blue-500',
            bgColor: 'bg-blue-500/10',
            link: '/slaughterhouse/products',
        },
        {
            title: 'Slaughter Lots',
            value: slaughterLots.length,
            icon: ClipboardList,
            color: 'text-green-500',
            bgColor: 'bg-green-500/10',
            link: '/slaughterhouse/lots',
        },
        {
            title: 'Chicken Receptions',
            value: receptions.length,
            icon: Truck,
            color: 'text-orange-500',
            bgColor: 'bg-orange-500/10',
            link: '/slaughterhouse/receptions',
        },
        {
            title: 'Active Products',
            value: products.filter(p => p.is_active).length,
            icon: TrendingUp,
            color: 'text-purple-500',
            bgColor: 'bg-purple-500/10',
            link: '/slaughterhouse/products',
        },
    ];

    return (
        <div className="space-y-6">
            {/* Header */}
            <div>
                <h1 className="text-3xl font-bold text-text">Slaughter House Dashboard</h1>
                <p className="text-text-muted mt-1">
                    Manage livestock reception, slaughter, and production
                </p>
            </div>

            {/* Stats Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                {stats.map((stat, index) => (
                    <Card
                        key={index}
                        className="p-6 hover:shadow-lg transition-shadow cursor-pointer"
                        onClick={() => navigate(stat.link)}
                    >
                        <div className="flex items-center justify-between">
                            <div>
                                <p className="text-sm text-text-muted">{stat.title}</p>
                                <p className="text-3xl font-bold mt-2">{stat.value}</p>
                            </div>
                            <div className={`p-3 rounded-lg ${stat.bgColor}`}>
                                <stat.icon className={`h-6 w-6 ${stat.color}`} />
                            </div>
                        </div>
                    </Card>
                ))}
            </div>

            {/* Quick Actions */}
            <Card className="p-6">
                <h2 className="text-xl font-bold mb-4">Quick Actions</h2>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <Button
                        variant="outline"
                        className="h-auto flex-col items-start p-4 hover:bg-primary/5"
                        onClick={() => navigate('/slaughterhouse/products')}
                    >
                        <div className="flex items-center gap-2 mb-2">
                            <Package className="h-5 w-5" />
                            <span className="font-semibold">Manage Products</span>
                        </div>
                        <p className="text-sm text-text-muted text-left">
                            View and manage all products
                        </p>
                        <ArrowRight className="h-4 w-4 mt-2 self-end" />
                    </Button>

                    <Button
                        variant="outline"
                        className="h-auto flex-col items-start p-4 hover:bg-primary/5"
                        onClick={() => navigate('/slaughterhouse/lots')}
                    >
                        <div className="flex items-center gap-2 mb-2">
                            <ClipboardList className="h-5 w-5" />
                            <span className="font-semibold">Slaughter Lots</span>
                        </div>
                        <p className="text-sm text-text-muted text-left">
                            Track slaughter lot records
                        </p>
                        <ArrowRight className="h-4 w-4 mt-2 self-end" />
                    </Button>

                    <Button
                        variant="outline"
                        className="h-auto flex-col items-start p-4 hover:bg-primary/5"
                        onClick={() => navigate('/slaughterhouse/receptions')}
                    >
                        <div className="flex items-center gap-2 mb-2">
                            <Truck className="h-5 w-5" />
                            <span className="font-semibold">Chicken Receptions</span>
                        </div>
                        <p className="text-sm text-text-muted text-left">
                            Record new chicken deliveries
                        </p>
                        <ArrowRight className="h-4 w-4 mt-2 self-end" />
                    </Button>
                </div>
            </Card>

            {/* Recent Activity */}
            <Card className="p-6">
                <h2 className="text-xl font-bold mb-4">Recent Activity</h2>
                <div className="space-y-3">
                    {products.length === 0 && slaughterLots.length === 0 && receptions.length === 0 ? (
                        <p className="text-text-muted text-center py-8">
                            No recent activity. Start by creating products, lots, or recording receptions.
                        </p>
                    ) : (
                        <>
                            {receptions.slice(0, 3).map((reception) => (
                                <div key={reception.id} className="flex items-center justify-between p-3 bg-muted/50 rounded">
                                    <div className="flex items-center gap-3">
                                        <Truck className="h-5 w-5 text-orange-500" />
                                        <div>
                                            <p className="font-medium">New Reception</p>
                                            <p className="text-sm text-text-muted">
                                                Lot ID: {reception.slaughter_lot_id} - {reception.quantity_received} chickens
                                            </p>
                                        </div>
                                    </div>
                                    <span className="text-sm text-text-muted">
                                        {reception.reception_date && new Date(reception.reception_date).toLocaleDateString()}
                                    </span>
                                </div>
                            ))}
                            {products.slice(0, 3).map((product) => (
                                <div key={product.id} className="flex items-center justify-between p-3 bg-muted/50 rounded">
                                    <div className="flex items-center gap-3">
                                        <Package className="h-5 w-5 text-blue-500" />
                                        <div>
                                            <p className="font-medium">Product Created</p>
                                            <p className="text-sm text-text-muted">
                                                {product.product_type} - {product.weight_value} kg
                                            </p>
                                        </div>
                                    </div>
                                    <span className="text-sm text-text-muted">
                                        {product.production_date && new Date(product.production_date).toLocaleDateString()}
                                    </span>
                                </div>
                            ))}
                        </>
                    )}
                </div>
            </Card>
        </div>
    );
}
