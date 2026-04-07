import client from '../../../shared/api/client';
import type { AuthTokenResponse, LoginRequest, SignupRequest, UserInfo } from '../types';

export const login = (data: LoginRequest) => 
  client.post<AuthTokenResponse>('/auth/login', data);

export const signup = (data: SignupRequest) => 
  client.post<void>('/auth/signup', data);

export const getMe = () => 
  client.get<UserInfo>('/auth/me');
