import { createRouter, createWebHistory } from 'vue-router';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/albums/:id',
      name: 'album-detail',
      component: () => import('../../features/album/pages/AlbumDetailPage.vue'),
    },
    {
      path: '/',
      name: 'dashboard',
      component: () => import('../../features/activity/pages/ActivityListPage.vue'),
    },
    {
      path: '/credits',
      name: 'credit',
      component: () => import('../../features/credit/pages/CreditPage.vue'),
    },
    {
      path: '/orders',
      name: 'orders-overview',
      component: () => import('../../features/order/pages/OrdersOverviewPage.vue'),
    },
    {
      path: '/books',
      name: 'books-overview',
      component: () => import('../../features/album/pages/BooksOverviewPage.vue'),
    },
    {
      path: '/albums/:albumId/orders',
      name: 'order-list',
      component: () => import('../../features/order/pages/OrderListPage.vue'),
    },
    {
      path: '/albums/:albumId/orders/:orderId',
      name: 'order-detail',
      component: () => import('../../features/order/pages/OrderDetailPage.vue'),
    },
  ],
});

export default router;
