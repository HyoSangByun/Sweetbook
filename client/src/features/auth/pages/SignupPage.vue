<template>
  <div class="auth-page">
    <div class="auth-container">
      <h1 class="auth-title">SweetBook</h1>
      <div class="auth-card">
        <h2 class="card-title">회원가입</h2>
        <form @submit.prevent="handleSignup" class="auth-form">
          <div class="form-group">
            <label for="email">이메일</label>
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
            <label for="password">비밀번호</label>
            <input 
              id="password" 
              v-model="password" 
              type="password" 
              placeholder="8자 이상" 
              required
              minlength="8"
              :disabled="isLoading"
            />
          </div>
          <div class="form-group">
            <label for="passwordConfirm">비밀번호 확인</label>
            <input 
              id="passwordConfirm" 
              v-model="passwordConfirm" 
              type="password" 
              placeholder="비밀번호 재입력" 
              required
              :disabled="isLoading"
            />
          </div>
          <div v-if="error" class="error-message">
            {{ error }}
          </div>
          <button type="submit" class="btn-primary" :disabled="isLoading">
            <span v-if="isLoading">가입 중...</span>
            <span v-else>회원가입</span>
          </button>
        </form>
        <div class="auth-footer">
          <p>이미 계정이 있으신가요? <router-link to="/login">로그인</router-link></p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../store';

const router = useRouter();
const authStore = useAuthStore();

const email = ref('');
const password = ref('');
const passwordConfirm = ref('');
const isLoading = ref(false);
const error = ref<string | null>(null);

const handleSignup = async () => {
  if (password.value !== passwordConfirm.value) {
    error.value = '비밀번호가 일치하지 않습니다.';
    return;
  }

  isLoading.value = true;
  error.value = null;
  try {
    await authStore.signup({
      email: email.value,
      password: password.value,
    });
    router.push({ name: 'dashboard' });
  } catch (err: any) {
    error.value = err.message || '회원가입에 실패했습니다. 다시 시도해주세요.';
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
