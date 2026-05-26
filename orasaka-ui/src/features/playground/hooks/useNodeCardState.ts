import React from "react";
import type { OperationNode } from "@/features/playground/types/playground.types";
import { useTranslation } from "@/core/context/LocaleContext";
import { useJobStream } from "@/features/jobs/context/JobStreamContext";
import { useNodeExecution } from "@/features/playground/hooks/useNodeExecution";
import { MediaApi } from "@/services/media.api";
import {
  NODE_ID,
  NODE_TO_CATEGORY,
  DEFAULT_PROVIDER,
  resolveMediaKind,
} from "@/core/constants/capability.constants";
import type { MediaKind } from "@/core/constants/capability.constants";

/**
 * Encapsulates all state management, model catalog fetching, execution logic,
 * and derived values for a PlaygroundNodeCard. Keeps the rendering component
 * under the 250-line threshold.
 */
export function useNodeCardState(node: OperationNode, onExecuted?: () => void) {
  const {
    playgroundInputs,
    setPlaygroundInput,
    playgroundResults,
    setPlaygroundResult,
    jobs,
    jobProgress,
    refreshJobs,
  } = useJobStream();

  const [modelCatalog, setModelCatalog] = React.useState<
    {
      id: number;
      modelName: string;
      modelLabel: string;
      category: string;
      providerName: string;
      options?: string;
    }[]
  >([]);

  const [configuredProviders, setConfiguredProviders] = React.useState<
    string[]
  >([]);

  const [uploadingFields, setUploadingFields] = React.useState<
    Record<string, boolean>
  >({});

  const currentModel = playgroundInputs[node.id]?.model || "";
  const currentVoice = playgroundInputs[node.id]?.voice || "";

  React.useEffect(() => {
    const fetchModels = async () => {
      try {
        const res = await fetch("/api/v1/models/catalog");
        if (res.ok) {
          const data = await res.json();
          const models = Array.isArray(data) ? data : (data.models || []);
          if (models.length > 0 || Array.isArray(data)) {
            setModelCatalog(models);
            const providerSet = new Set<string>();
            for (const m of models) {
              if (m.providerName) providerSet.add(m.providerName);
            }
            setConfiguredProviders(Array.from(providerSet));
          }
        }
      } catch {
        // Catalog unavailable — use fallback defaults
      }
    };
    fetchModels();
  }, []);

  React.useEffect(() => {
    if (currentModel && modelCatalog.length > 0) {
      const dbModel = modelCatalog.find((m) => m.modelName === currentModel);
      if (dbModel?.options && node.id === NODE_ID.CHAT_SPEECH) {
        const voiceOptions = dbModel.options.split(",");
        if (!currentVoice || !voiceOptions.includes(currentVoice)) {
          setPlaygroundInput(node.id, "voice", voiceOptions[0]);
        }
      }
    }
  }, [node.id, currentModel, modelCatalog, currentVoice, setPlaygroundInput]);

  const inputs = playgroundInputs[node.id] || {};
  const globalResult = playgroundResults[node.id] || null;

  const { execute, isPending } = useNodeExecution({
    onSuccess: (data) => {
      setPlaygroundResult(node.id, { success: true, data });
      refreshJobs();
      if (onExecuted) onExecuted();
    },
    onError: (err) => {
      setPlaygroundResult(node.id, { success: false, error: err.message });
    },
  });

  const { t } = useTranslation();

  const kind: MediaKind = resolveMediaKind(node.id, node.icon);

  const template = node.executionDetails.payloadTemplate;

  const rawPlaceholders = template
    ? (template.match(/\${(.*?)}/g) || []).map((m) => m.replace(/\${|}/g, ""))
    : [];

  const placeholderDefaults: Record<string, string> = {};
  const placeholders = rawPlaceholders.map((raw) => {
    const colonIdx = raw.indexOf(":");
    if (colonIdx !== -1) {
      const name = raw.substring(0, colonIdx);
      placeholderDefaults[name] = raw.substring(colonIdx + 1);
      return name;
    }
    return raw;
  });

  const category = NODE_TO_CATEGORY[node.id] ?? "";

  const categoryModels = modelCatalog.filter((m) => m.category === category);

  const uniqueProviders = Array.from(
    new Set([
      ...categoryModels.map((m) => m.providerName).filter(Boolean),
      ...configuredProviders,
    ]),
  ) as string[];
  if (uniqueProviders.length === 0) {
    uniqueProviders.push(DEFAULT_PROVIDER);
  }

  const activeModelName = inputs["model"] || placeholderDefaults["model"] || "";
  const currentDbModel = modelCatalog.find(
    (m) => m.modelName === activeModelName,
  );
  const currentProvider = currentDbModel?.providerName || uniqueProviders[0];

  const handleInputChange = (field: string, value: string) => {
    setPlaygroundInput(node.id, field, value);
  };

  const handleFileUpload = (field: string, file: File) => {
    setUploadingFields((prev) => ({ ...prev, [field]: true }));
    MediaApi.uploadMedia(file)
      .then((res) => {
        handleInputChange(field, res.assetId);
      })
      .catch((err) => {
        alert(t.playground.failedUploadMedia + err.message);
      })
      .finally(() => {
        setUploadingFields((prev) => ({ ...prev, [field]: false }));
      });
  };

  const executeNode = async () => {
    let payload = "";
    if (template) {
      let temp = template;
      for (const raw of rawPlaceholders) {
        const colonIdx = raw.indexOf(":");
        const fieldName = colonIdx !== -1 ? raw.substring(0, colonIdx) : raw;
        const defaultVal = colonIdx !== -1 ? raw.substring(colonIdx + 1) : "";
        const value = inputs[fieldName] || defaultVal;
        temp = temp.replace(`\${${raw}}`, value);
      }
      payload = temp;
    }
    execute({
      uriPath: node.executionDetails.uriPath,
      httpMethod: node.executionDetails.httpMethod,
      payload,
    });
  };

  let jobId: string | undefined = undefined;
  if (globalResult && globalResult.success) {
    try {
      const parsed = JSON.parse(globalResult.data);
      if (parsed && typeof parsed === "object" && "jobId" in parsed) {
        jobId = parsed.jobId;
      }
    } catch {
      // Not a JSON string
    }
  }

  const activeJob = jobId ? jobs.find((j) => j.id === jobId) : null;
  const activeProgress = jobId ? jobProgress[jobId] : undefined;
  const isJobRunning = activeJob
    ? activeJob.status === "PENDING" || activeJob.status === "PROCESSING"
    : false;
  const anyUploading = Object.values(uploadingFields).some(Boolean);
  const displayPending = isPending || isJobRunning || anyUploading;

  const isLocked = node.state.type === "LOCKED";

  return {
    t,
    kind,
    inputs,
    globalResult,
    placeholders,
    category,
    currentProvider,
    uniqueProviders,
    modelCatalog,
    uploadingFields,
    currentModel,
    activeProgress,
    displayPending,
    isLocked,
    handleInputChange,
    handleFileUpload,
    executeNode,
  };
}
