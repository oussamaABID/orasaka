import type { BootstrapFeature } from "@/features/chat-session/components/ContextPlusMenu";
import type { MediaKind } from "@/core/constants/capability.constants";
import {
  resolveDefaultModel,
  resolveMediaKind,
  DEFAULT_VOICE,
} from "@/core/constants/capability.constants";

/**
 * Executes a feature capability with a compiled payload template.
 *
 * Extracts UUID from the prompt if present, resolves the default model and voice,
 * compiles the template with variable substitution, and invokes the feature endpoint.
 */
export const executeFeatureWithPrompt = async ({
  feature,
  prompt,
  assetId,
}: Readonly<{
  feature: BootstrapFeature;
  prompt: string;
  assetId?: string;
}>): Promise<{ content: string; kind: MediaKind }> => {
  const uuidRegex =
    /[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i;
  const match = prompt.match(uuidRegex);
  const detectedUuid = match
    ? match[0]
    : assetId || "00000000-0000-0000-0000-000000000000";

  const model = resolveDefaultModel(feature.id);
  const voice = DEFAULT_VOICE;
  const image = detectedUuid;
  const assetIdVal = detectedUuid;

  let payload = feature.payloadTemplate || JSON.stringify({ prompt });
  payload = payload.replace(/\$\{prompt\}/g, prompt);
  payload = payload.replace(/\$\{text\}/g, prompt);
  payload = payload.replace(/\$\{model\}/g, model);
  payload = payload.replace(/\$\{voice\}/g, voice);
  payload = payload.replace(/\$\{image\}/g, image);
  payload = payload.replace(/\$\{assetId\}/g, assetIdVal);

  const response = await fetch(feature.uriPath, {
    method: feature.httpMethod,
    headers: { "Content-Type": "application/json" },
    body: feature.httpMethod !== "GET" ? payload : undefined,
  });

  if (!response.ok) {
    throw new Error(
      response.status === 403
        ? "Access Forbidden: Restricted by gateway protection policy."
        : `Execution failed with status ${response.status}`,
    );
  }

  const contentType = response.headers.get("content-type") || "";
  let content = "";
  if (contentType.includes("application/json")) {
    const json = await response.json();
    if (json.jobId && json.status) {
      content = `⏳ Task queued (Job: ${json.jobId.substring(0, 8)}...). Check the notification bell for progress.`;
    } else {
      content = json.content || json.analysis || JSON.stringify(json, null, 2);
    }
  } else {
    content = await response.text();
  }

  const kind: MediaKind = resolveMediaKind(feature.id, feature.icon);

  return { content, kind };
};
