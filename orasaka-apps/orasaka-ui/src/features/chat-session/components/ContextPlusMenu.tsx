import React, { useRef, useEffect } from "react";

export interface BootstrapFeature {
  id: string;
  label: string;
  icon: string;
  uriPath: string;
  httpMethod: string;
  payloadTemplate: string;
}

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onExecuteNode: (feature: BootstrapFeature) => void;
  features: BootstrapFeature[];
}

export const ContextPlusMenu: React.FC<Props> = ({
  isOpen,
  onClose,
  onExecuteNode,
  features,
}) => {
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        const target = event.target as HTMLElement;
        const button = target.closest("button");
        if (
          button &&
          (button
            .getAttribute("aria-label")
            ?.toLowerCase()
            .includes("capability") ||
            button
              .getAttribute("aria-label")
              ?.toLowerCase()
              .includes("fonctionnalité") ||
            button.querySelector("svg")?.classList.contains("rotate-45"))
        ) {
          return;
        }
        onClose();
      }
    };

    if (isOpen) {
      document.addEventListener("mousedown", handleClickOutside);
    }
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handleSelect = (feature: BootstrapFeature) => {
    onExecuteNode(feature);
    onClose();
  };

  const getEmojiIcon = (iconName: string) => {
    switch (iconName.toLowerCase()) {
      case "image":
        return "🎨";
      case "mic":
        return "🎙️";
      case "video":
        return "🎥";
      case "chat":
        return "💬";
      default:
        return "💬";
    }
  };

  return (
    <div
      ref={menuRef}
      className="absolute bottom-16 left-4 z-40 w-64 bg-white/95 dark:bg-zinc-900/95 border border-zinc-200/80 dark:border-zinc-800/60 rounded-2xl shadow-xl backdrop-blur-md p-2 flex flex-col gap-1 animate-in slide-in-from-bottom-2 duration-200"
    >
      <div className="px-3 py-1.5 text-[10px] font-semibold text-zinc-400 dark:text-zinc-500 uppercase tracking-wider border-b border-zinc-100 dark:border-zinc-800 mb-1">
        Capabilities
      </div>
      {features.length === 0 ? (
        <div className="px-3 py-2 text-xs text-zinc-400 dark:text-zinc-500 italic">
          No extra tools available
        </div>
      ) : (
        features.map((feature) => (
          <button
            key={feature.id}
            type="button"
            onClick={() => handleSelect(feature)}
            className="w-full flex items-center gap-3 px-3 py-2 rounded-xl text-xs font-medium text-zinc-700 hover:text-zinc-900 hover:bg-zinc-50 dark:text-zinc-300 dark:hover:text-zinc-50 dark:hover:bg-zinc-800/60 transition-all duration-150 text-left"
          >
            <span className="text-base select-none">
              {getEmojiIcon(feature.icon)}
            </span>
            <span className="flex-1">{feature.label}</span>
          </button>
        ))
      )}
    </div>
  );
};
