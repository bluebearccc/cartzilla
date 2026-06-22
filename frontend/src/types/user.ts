import type { Role } from './api';

export interface Profile {
  id: string;
  email: string;
  fullName: string;
  phone: string | null;
  role: Role;
  emailVerified: boolean;
  active: boolean;
}

export interface Address {
  id: string;
  fullName: string;
  phone: string;
  street: string;
  district: string;
  city: string;
  defaultAddress: boolean;
}

export interface AddressInput {
  fullName: string;
  phone: string;
  street: string;
  district: string;
  city: string;
  defaultAddress: boolean;
}
