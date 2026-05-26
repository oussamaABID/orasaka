"use client";

import { PlaygroundCapabilityPage } from "@/features/playground/components/PlaygroundCapabilityPage";

export default function ImageGeneratePage() {
  return (
    <PlaygroundCapabilityPage
      nodeId="orasaka.core.chat.image"
      titleAccessor={(t) => t.sidebar.generateImage}
    />
  );
}
