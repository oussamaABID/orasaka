export interface OrasakaUser {
  id: string;
  name?: string | null;
  email?: string | null;
  image?: string | null;
  role?: string;
}

export interface AuthState {
  user: OrasakaUser | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}
