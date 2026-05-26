"use client";

import { PlaygroundCapabilityPage } from "@/features/playground/components/PlaygroundCapabilityPage";

export default function AudioAnalyzePage() {
  return (
    <PlaygroundCapabilityPage
      nodeId="orasaka.core.media.audio"
      titleAccessor={(t) => t.sidebar.analyzeAudio}
    />
  );
}
