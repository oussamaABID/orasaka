import { useSession, signIn, signOut } from "next-auth/react";

export function useAuth() {
  const { data: session, status } = useSession();

  const isAuthenticated = status === "authenticated";
  const isLoading = status === "loading";
  const user = session?.user;

  const loginWithGithub = () => signIn("github", { callbackUrl: "/" });
  const loginWithGoogle = () => signIn("google", { callbackUrl: "/" });
  
  // Example for credentials/orasaka-gateway via TanStack query logic vs next-auth
  // For credentials, you'd typically call signIn("credentials", { ... })
  
  const logout = () => signOut({ callbackUrl: "/login" });

  return {
    session,
    user,
    isAuthenticated,
    isLoading,
    loginWithGithub,
    loginWithGoogle,
    logout,
  };
}
