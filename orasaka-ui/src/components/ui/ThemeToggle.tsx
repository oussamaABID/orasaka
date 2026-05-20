'use client';

import * as React from 'react';
import { Moon, Sun, Monitor } from 'lucide-react';
import { useTheme } from 'next-themes';
import { Button } from '@/components/ui/Button';

export function ThemeToggle() {
  const { setTheme, theme } = useTheme();
  const [mounted, setMounted] = React.useState(false);

  React.useEffect(() => {
    setMounted(true);
  }, []);

  if (!mounted) {
    return <div className="h-9 w-[120px] rounded-md bg-zinc-100 dark:bg-zinc-800 animate-pulse" />;
  }

  return (
    <div className="flex items-center rounded-md border border-zinc-200 bg-white p-1 dark:border-zinc-800 dark:bg-zinc-950">
      <Button
        variant="ghost"
        size="sm"
        className={`h-7 px-2 ${theme === 'light' ? 'bg-zinc-100 dark:bg-zinc-800' : ''}`}
        onClick={() => setTheme('light')}
      >
        <Sun className="h-4 w-4" />
      </Button>
      <Button
        variant="ghost"
        size="sm"
        className={`h-7 px-2 ${theme === 'system' ? 'bg-zinc-100 dark:bg-zinc-800' : ''}`}
        onClick={() => setTheme('system')}
      >
        <Monitor className="h-4 w-4" />
      </Button>
      <Button
        variant="ghost"
        size="sm"
        className={`h-7 px-2 ${theme === 'dark' ? 'bg-zinc-100 dark:bg-zinc-800' : ''}`}
        onClick={() => setTheme('dark')}
      >
        <Moon className="h-4 w-4" />
      </Button>
    </div>
  );
}
