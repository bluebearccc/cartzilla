import { api } from '@/lib/api';
import type { Address, AddressInput, Profile } from '@/types/user';

export const userApi = {
  getProfile: () => api.get<Profile>('/users/me'),
  updateProfile: (data: { fullName: string; phone: string }) =>
    api.put<Profile>('/users/me', data),
  changePassword: (currentPassword: string, newPassword: string) =>
    api.put<void>('/users/me/password', { currentPassword, newPassword }),

  listAddresses: () => api.get<Address[]>('/users/me/addresses'),
  createAddress: (data: AddressInput) => api.post<Address>('/users/me/addresses', data),
  updateAddress: (id: string, data: AddressInput) =>
    api.put<Address>(`/users/me/addresses/${id}`, data),
  setDefaultAddress: (id: string) => api.put<Address>(`/users/me/addresses/${id}/default`),
  deleteAddress: (id: string) => api.del<void>(`/users/me/addresses/${id}`),
};
