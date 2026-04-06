import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '../../features/auth/store';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../../features/auth/pages/LoginPage.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/signup',
      name: 'signup',
      component: () => import('../../features/auth/pages/SignupPage.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/',
      name: 'dashboard',
      component: () => import('../../features/activity/pages/ActivityListPage.vue'),
      meta: { requiresAuth: true },
    },
  ],
});

router.beforeEach(async (to, _from, next) => {
  const authStore = useAuthStore();
  
  if (!authStore.isInitialised) {
    try {
      await authStore.bootstrap();
    } catch (err) {
      console.error('Failed to initialise auth store:', err);
    }
  }

  const isAuthenticated = !!authStore.token;

  if (to.meta.requiresAuth && !isAuthenticated) {
    next({ name: 'login' });
  } else if (to.meta.guestOnly && isAuthenticated) {
    next({ name: 'dashboard' });
  } else {
    next();
  }
});

export default router;
