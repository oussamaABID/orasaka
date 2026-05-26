"use client";

import { PlaygroundCapabilityPage } from "@/features/playground/components/PlaygroundCapabilityPage";

export default function TextChatPage() {
  return (
    <PlaygroundCapabilityPage
      nodeId="orasaka.core.chat.text"
      titleAccessor={(t) => t.sidebar.textChat}
    />
  );
}
