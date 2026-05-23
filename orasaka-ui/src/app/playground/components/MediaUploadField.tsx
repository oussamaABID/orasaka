import React from "react";
import { Upload } from "lucide-react";

/**
 * Props definition for the MediaUploadField component.
 */
interface MediaUploadFieldProps {
  field: string;
  inputs: Record<string, string>;
  isLocked: boolean;
  handleInputChange: (field: string, value: string) => void;
  handleFileUpload: (field: string, file: File) => void;
}

/**
 * Component to display drag-and-drop or file upload controls with inline asset previews.
 */
export function MediaUploadField({
  field,
  inputs,
  isLocked,
  handleInputChange,
  handleFileUpload,
}: MediaUploadFieldProps) {
  const isAudio = field.toLowerCase().includes("audio");
  const previewUrl = inputs[field]
    ? `data:${isAudio ? "audio/mp3" : "image/png"};base64,${inputs[field]}`
    : "";

  return (
    <div className="flex flex-col gap-2">
      <span className="text-xs font-semibold text-zinc-500 uppercase tracking-wider">
        Media Payload ({field})
      </span>
      <div className="border-2 border-dashed border-zinc-200 dark:border-zinc-800 rounded-xl p-4 flex flex-col items-center justify-center bg-zinc-50/30 dark:bg-zinc-950/10 min-h-[140px]">
        {previewUrl ? (
          <div className="flex flex-col items-center gap-2 w-full">
            {isAudio ? (
              <audio src={previewUrl} controls className="w-full max-h-12" />
            ) : (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={previewUrl}
                alt="Preview"
                className="max-h-32 rounded-lg object-contain"
              />
            )}
            <button
              onClick={() => handleInputChange(field, "")}
              className="text-xs text-red-500 hover:underline"
              disabled={isLocked}
            >
              Remove Asset
            </button>
          </div>
        ) : (
          <div className="flex flex-col items-center gap-2">
            <Upload className="h-8 w-8 text-zinc-400 animate-pulse" />
            <p className="text-xs text-zinc-500 text-center">
              Drag and drop asset, or{" "}
              <label
                className={`text-amber-500 ${isLocked ? "pointer-events-none opacity-50" : "cursor-pointer"} hover:underline`}
              >
                browse
                <input
                  type="file"
                  disabled={isLocked}
                  onChange={(e) =>
                    e.target.files?.[0] &&
                    handleFileUpload(field, e.target.files[0])
                  }
                  accept={isAudio ? "audio/*" : "image/*"}
                  className="hidden"
                />
              </label>
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
