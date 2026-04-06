import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { LoginRequest, SignupRequest, UserInfo } from '../types';
import * as authApi from '../api/authApi';

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('accessToken'));
  const me = ref<UserInfo | null>(null);
  const isInitialised = ref(false);

  const setToken = (newToken: string) => {
    token.value = newToken;
    localStorage.setItem('accessToken', newToken);
  };

  const clearAuth = () => {
    token.value = null;
    me.value = null;
    localStorage.removeItem('accessToken');
  };

  const login = async (data: LoginRequest) => {
    const res = await authApi.login(data);
    setToken(res.accessToken);
    await fetchMe();
  };

  const signup = async (data: SignupRequest) => {
    const res = await authApi.signup(data);
    setToken(res.accessToken);
    await fetchMe();
  };

  const fetchMe = async () => {
    try {
      const res = await authApi.getMe();
      me.value = res;
    } catch (err) {
      clearAuth();
      throw err;
    }
  };

  const logout = () => {
    clearAuth();
  };

  const bootstrap = async () => {
    if (token.value) {
      try {
        await fetchMe();
      } catch (err) {
        clearAuth();
      }
    }
    isInitialised.value = true;
  };

  return {
    token,
    me,
    isInitialised,
    login,
    signup,
    logout,
    bootstrap,
  };
});
