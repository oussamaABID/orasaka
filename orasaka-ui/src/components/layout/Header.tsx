import * as React from 'react';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { ThemeToggle } from '@/components/ui/ThemeToggle';

export function Header() {
  const { user, isAuthenticated } = useAuth();

  return (
    <header className="flex h-16 w-full items-center justify-between border-b border-zinc-200 bg-white px-6 dark:border-zinc-800 dark:bg-zinc-900">
      <div className="flex-1" /> {/* Spacer */}
      <div className="flex items-center space-x-6">
        <ThemeToggle />
        {isAuthenticated && user && (
          <div className="flex items-center space-x-3">
            <span className="text-sm font-medium text-zinc-700 dark:text-zinc-300">
              {user.name || user.email || 'Admin'}
            </span>
            <div className="h-8 w-8 rounded-full bg-zinc-200 dark:bg-zinc-700" />
          </div>
        )}
      </div>
    </header>
  );
}
