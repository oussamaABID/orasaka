"use client";

import { PlaygroundCapabilityPage } from "@/features/playground/components/PlaygroundCapabilityPage";

export default function SpeechSynthesisPage() {
  return (
    <PlaygroundCapabilityPage
      nodeId="orasaka.core.chat.speech"
      titleAccessor={(t) => t.sidebar.speechSynthesis}
    />
  );
}
