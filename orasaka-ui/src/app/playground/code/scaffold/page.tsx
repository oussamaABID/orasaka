"use client";

import { PlaygroundCapabilityPage } from "@/features/playground/components/PlaygroundCapabilityPage";

export default function CodeScaffoldPage() {
  return (
    <PlaygroundCapabilityPage
      nodeId="orasaka.core.chat.code"
      titleAccessor={(t) => t.sidebar.featureToCode}
    />
  );
}
