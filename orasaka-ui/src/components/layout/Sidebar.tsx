import * as React from 'react';
import Link from 'next/link';
import { Home, MessageSquare, Settings, LogOut } from 'lucide-react';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { Button } from '@/components/ui/Button';

export function Sidebar() {
  const { logout } = useAuth();

  return (
    <div className="flex h-full w-64 flex-col border-r border-zinc-200 bg-zinc-50 dark:border-zinc-800 dark:bg-zinc-950">
      <div className="flex h-16 items-center px-6 border-b border-zinc-200 dark:border-zinc-800">
        <h1 className="text-xl font-bold text-zinc-900 dark:text-zinc-50">ORASAKA</h1>
      </div>
      <div className="flex-1 overflow-auto py-4">
        <nav className="space-y-1 px-4">
          <Link href="/">
            <Button variant="ghost" className="w-full justify-start text-zinc-600 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-50">
              <Home className="mr-3 h-5 w-5" />
              Dashboard
            </Button>
          </Link>
          <Link href="/chat">
            <Button variant="ghost" className="w-full justify-start text-zinc-600 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-50">
              <MessageSquare className="mr-3 h-5 w-5" />
              Chat Sessions
            </Button>
          </Link>
          <Link href="/settings">
            <Button variant="ghost" className="w-full justify-start text-zinc-600 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-50">
              <Settings className="mr-3 h-5 w-5" />
              Settings
            </Button>
          </Link>
        </nav>
      </div>
      <div className="p-4 border-t border-zinc-200 dark:border-zinc-800">
        <Button variant="outline" className="w-full justify-start text-red-600 hover:bg-red-50 hover:text-red-700 dark:border-red-900 dark:text-red-500 dark:hover:bg-red-950" onClick={logout}>
          <LogOut className="mr-3 h-5 w-5" />
          Log out
        </Button>
      </div>
    </div>
  );
}
