import React from "react";

export interface TargetExecutionUri {
  uriPath: string;
  httpMethod: string;
  payloadTemplate?: string;
}

export interface NodeState {
  type: "ACTIVE" | "LOCKED" | "INVISIBLE";
  reason?: string;
  lockedAt?: string;
}

export interface OperationNode {
  id: string;
  label: string;
  icon: string;
  presentationContext: string;
  state: NodeState;
  executionDetails: TargetExecutionUri;
}

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onExecuteNode: (node: OperationNode) => void;
  nodes: OperationNode[];
}

export const ContextPlusMenu: React.FC<Props> = ({
  isOpen,
  onClose,
  onExecuteNode,
  nodes,
}) => {
  if (!isOpen) return null;

  const handleSelect = (node: OperationNode) => {
    onExecuteNode(node);
    onClose();
  };

  const getEmojiIcon = (iconName: string) => {
    switch (iconName.toLowerCase()) {
      case "image":
        return "🎨";
      case "mic":
        return "🎙️";
      default:
        return "💬";
    }
  };

  return (
    <div className="absolute bottom-16 left-4 z-40 w-64 bg-white/95 dark:bg-zinc-900/95 border border-zinc-200/80 dark:border-zinc-800/60 rounded-2xl shadow-xl backdrop-blur-md p-2 flex flex-col gap-1 animate-in slide-in-from-bottom-2 duration-200">
      <div className="px-3 py-1.5 text-[10px] font-semibold text-zinc-400 dark:text-zinc-500 uppercase tracking-wider border-b border-zinc-100 dark:border-zinc-800 mb-1">
        Capabilities
      </div>
      {nodes.length === 0 ? (
        <div className="px-3 py-2 text-xs text-zinc-400 dark:text-zinc-500 italic">
          No extra tools available
        </div>
      ) : (
        nodes.map((node) => {
          const isActive = node.state.type === "ACTIVE";

          if (node.state.type === "INVISIBLE") {
            return null;
          }

          if (isActive) {
            return (
              <button
                key={node.id}
                type="button"
                onClick={() => handleSelect(node)}
                className="w-full flex items-center gap-3 px-3 py-2 rounded-xl text-xs font-medium text-zinc-700 hover:text-zinc-900 hover:bg-zinc-50 dark:text-zinc-300 dark:hover:text-zinc-50 dark:hover:bg-zinc-800/60 transition-all duration-150 text-left"
              >
                <span className="text-base select-none">
                  {getEmojiIcon(node.icon)}
                </span>
                <span className="flex-1">{node.label}</span>
              </button>
            );
          }

          return (
            <div
              key={node.id}
              className="group relative w-full flex items-center gap-3 px-3 py-2 rounded-xl text-xs font-medium text-zinc-400 bg-zinc-50/50 dark:bg-zinc-950/20 dark:text-zinc-600 border border-dashed border-zinc-200 dark:border-zinc-800 cursor-not-allowed select-none"
            >
              <span className="text-base select-none opacity-50">
                {getEmojiIcon(node.icon)}
              </span>
              <span className="flex-1">{node.label}</span>
              <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                strokeWidth={1.5}
                stroke="currentColor"
                className="w-3.5 h-3.5 text-zinc-400 dark:text-zinc-600 flex-shrink-0"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M16.5 10.5V6.75a4.5 4.5 0 1 0-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 0 0 2.25-2.25v-6.75a2.25 2.25 0 0 0-2.25-2.25H6.75a2.25 2.25 0 0 0-2.25 2.25v6.75a2.25 2.25 0 0 0 2.25 2.25Z"
                />
              </svg>
              <div className="absolute left-[102%] top-0 w-48 p-2.5 bg-zinc-950/95 dark:bg-zinc-800/95 text-white dark:text-zinc-100 text-[10px] rounded-lg shadow-lg border border-zinc-800/40 opacity-0 pointer-events-none group-hover:opacity-100 transition-opacity duration-150 z-50">
                {node.state.reason || "This feature is locked by policy."}
              </div>
            </div>
          );
        })
      )}
    </div>
  );
};
