<template>
  <div class="auth-page">
    <div class="auth-container">
      <h1 class="auth-title">SweetBook</h1>
      <div class="auth-card">
        <h2 class="card-title">Login</h2>
        <div v-if="showSignupSuccess" class="success-message" role="status" aria-live="polite">
          <span>Signup completed successfully. Please log in.</span>
          <button
            type="button"
            class="btn-dismiss"
            @click="dismissSignupSuccess"
            :disabled="isDismissingSignupSuccess"
            :aria-busy="isDismissingSignupSuccess"
          >
            Close
          </button>
        </div>
        <form @submit.prevent="handleLogin" class="auth-form">
          <div class="form-group">
            <label for="email">Email</label>
            <input
              id="email"
              v-model="email"
              type="email"
              placeholder="email@example.com"
              required
              :disabled="isLoading"
            />
          </div>
          <div class="form-group">
            <label for="password">Password</label>
            <input
              id="password"
              v-model="password"
              type="password"
              placeholder="********"
              required
              :disabled="isLoading"
            />
          </div>
          <div v-if="error" class="error-message">
            {{ error }}
          </div>
          <button type="submit" class="btn-primary" :disabled="isLoading">
            <span v-if="isLoading">Logging in...</span>
            <span v-else>Login</span>
          </button>
        </form>
        <div class="auth-footer">
          <p>Don't have an account? <router-link to="/signup">Sign up</router-link></p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../store';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const email = ref('');
const password = ref('');
const isLoading = ref(false);
const error = ref<string | null>(null);
const isSignupSuccessDismissed = ref(false);
const isDismissingSignupSuccess = ref(false);

const isSignupSuccess = computed(() => route.query.signup === 'success');
const showSignupSuccess = computed(() => isSignupSuccess.value && !isSignupSuccessDismissed.value);

const dismissSignupSuccess = async () => {
  if (isDismissingSignupSuccess.value) return;
  isDismissingSignupSuccess.value = true;
  try {
    isSignupSuccessDismissed.value = true;
    const nextQuery = { ...route.query };
    delete nextQuery.signup;
    await router.replace({ query: nextQuery });
  } finally {
    isDismissingSignupSuccess.value = false;
  }
};

let clearSuccessTimer: ReturnType<typeof setTimeout> | null = null;

watch(
  showSignupSuccess,
  (visible) => {
    if (clearSuccessTimer) {
      clearTimeout(clearSuccessTimer);
      clearSuccessTimer = null;
    }

    if (visible) {
      clearSuccessTimer = setTimeout(() => {
        dismissSignupSuccess();
      }, 5000);
    }
  },
  { immediate: true },
);

onBeforeUnmount(() => {
  if (clearSuccessTimer) {
    clearTimeout(clearSuccessTimer);
    clearSuccessTimer = null;
  }
});

const handleLogin = async () => {
  if (isLoading.value) return;
  isLoading.value = true;
  error.value = null;
  try {
    await authStore.login({
      email: email.value,
      password: password.value,
    });
    router.push({ name: 'dashboard' });
  } catch (err: any) {
    error.value = err.message || 'Login failed. Please try again.';
  } finally {
    isLoading.value = false;
  }
};
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--color-parchment);
}

.auth-container {
  width: 100%;
  max-width: 400px;
  padding: 24px;
}

.auth-title {
  text-align: center;
  font-size: 3rem;
  margin-bottom: 2rem;
  color: var(--color-near-black);
}

.auth-card {
  background-color: var(--color-ivory);
  padding: 32px;
  border-radius: 16px;
  border: 1px solid var(--color-border-cream);
  box-shadow: rgba(0, 0, 0, 0.05) 0px 4px 24px;
}

.card-title {
  font-size: 1.5rem;
  margin-bottom: 1.5rem;
  text-align: center;
}

.success-message {
  margin-bottom: 16px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #e7f7eb;
  color: #1f6132;
  font-size: 0.875rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.btn-dismiss {
  background: transparent;
  color: #1f6132;
  padding: 2px 6px;
  font-size: 0.75rem;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

label {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-olive-gray);
}

input {
  width: 100%;
  box-sizing: border-box;
}

.btn-primary {
  background-color: var(--color-terracotta);
  color: var(--color-white);
  padding: 12px;
  font-size: 1rem;
  font-weight: 500;
  border-radius: 12px;
  margin-top: 8px;
}

.btn-primary:hover:not(:disabled) {
  opacity: 0.9;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error-message {
  color: var(--color-error);
  font-size: 0.875rem;
  text-align: center;
}

.auth-footer {
  margin-top: 24px;
  text-align: center;
  font-size: 0.875rem;
  color: var(--color-olive-gray);
}

.auth-footer a {
  color: var(--color-terracotta);
  font-weight: 500;
}
</style>
