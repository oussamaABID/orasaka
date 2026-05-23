/**
 * User representation inside the frontend authentication context.
 */
export interface OrasakaUser {
  id: string;
  name?: string | null;
  email?: string | null;
  image?: string | null;
  role?: string;
}

/**
 * Authentication state wrapping loading status and current logged-in user context.
 */
export interface AuthState {
  user: OrasakaUser | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}
