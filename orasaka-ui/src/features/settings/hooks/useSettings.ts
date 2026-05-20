import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { OrasakaSettings } from '../types/settings.types';

// Mock API Call (Replaces actual GraphQL `me` query later)
const fetchSettings = async (): Promise<OrasakaSettings> => {
  return new Promise((resolve) =>
    setTimeout(() => {
      resolve({
        language: 'en',
        autoSave: true,
        aiPersona: 'standard',
      });
    }, 500)
  );
};

// Mock Mutation
const updateSettings = async (settings: Partial<OrasakaSettings>) => {
  return new Promise((resolve) => setTimeout(() => resolve(settings), 500));
};

export function useSettings() {
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: ['settings'],
    queryFn: fetchSettings,
  });

  const mutation = useMutation({
    mutationFn: updateSettings,
    onSuccess: (updatedSettings) => {
      // Invalidate or optimistically update the 'settings' query
      queryClient.setQueryData(['settings'], (old: OrasakaSettings) => ({
        ...old,
        ...updatedSettings,
      }));
    },
  });

  return {
    settings: query.data,
    isLoading: query.isLoading,
    updateSettings: mutation.mutate,
    isUpdating: mutation.isPending,
  };
}
