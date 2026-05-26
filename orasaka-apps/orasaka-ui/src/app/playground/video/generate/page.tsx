"use client";

import { PlaygroundCapabilityPage } from "@/features/playground/components/PlaygroundCapabilityPage";

export default function VideoGeneratePage() {
  return (
    <PlaygroundCapabilityPage
      nodeId="orasaka.core.media.video"
      titleAccessor={(t) => t.sidebar.generateVideo}
    />
  );
}
