'use client';

import * as React from 'react';
import { useSettings } from '../hooks/useSettings';
import { Card, CardContent, CardDescription, CardHeader, CardTitle, CardFooter } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';

export function SettingsForm() {
  const { settings, isLoading, updateSettings, isUpdating } = useSettings();
  
  const [language, setLanguage] = React.useState('en');
  const [aiPersona, setAiPersona] = React.useState('standard');

  // Sync state once loaded
  React.useEffect(() => {
    if (settings) {
      setLanguage(settings.language);
      setAiPersona(settings.aiPersona);
    }
  }, [settings]);

  if (isLoading) {
    return <div className="animate-pulse h-64 bg-zinc-100 dark:bg-zinc-900 rounded-lg" />;
  }

  const handleSave = () => {
    updateSettings({ language, aiPersona: aiPersona as any });
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>User Preferences</CardTitle>
        <CardDescription>
          Manage your AI preferences and gateway settings here.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        <div className="space-y-2">
          <label className="text-sm font-medium">Language</label>
          <select 
            className="flex h-10 w-full rounded-md border border-zinc-200 bg-white px-3 py-2 text-sm dark:border-zinc-800 dark:bg-zinc-950"
            value={language}
            onChange={(e) => setLanguage(e.target.value)}
          >
            <option value="en">English</option>
            <option value="fr">Français</option>
          </select>
        </div>

        <div className="space-y-2">
          <label className="text-sm font-medium">AI Persona</label>
          <select 
            className="flex h-10 w-full rounded-md border border-zinc-200 bg-white px-3 py-2 text-sm dark:border-zinc-800 dark:bg-zinc-950"
            value={aiPersona}
            onChange={(e) => setAiPersona(e.target.value)}
          >
            <option value="standard">Standard Orasaka</option>
            <option value="concise">Concise & Direct</option>
            <option value="creative">Creative & Exploratory</option>
          </select>
        </div>
      </CardContent>
      <CardFooter>
        <Button onClick={handleSave} disabled={isUpdating}>
          {isUpdating ? 'Saving...' : 'Save Preferences'}
        </Button>
      </CardFooter>
    </Card>
  );
}
