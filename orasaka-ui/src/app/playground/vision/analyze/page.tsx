"use client";

import { PlaygroundCapabilityPage } from "@/features/playground/components/PlaygroundCapabilityPage";

export default function VisionAnalyzePage() {
  return (
    <PlaygroundCapabilityPage
      nodeId="orasaka.core.media.vision"
      titleAccessor={(t) => t.sidebar.visionAnalysis}
    />
  );
}
