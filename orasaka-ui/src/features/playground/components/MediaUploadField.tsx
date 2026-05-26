"use client";

import React, { useState, useEffect, useRef } from "react";
import { Upload, Loader2 } from "lucide-react";
import { useTranslation } from "@/core/context/LocaleContext";

interface MediaUploadFieldProps {
  field: string;
  inputs: Record<string, string>;
  isLocked: boolean;
  handleInputChange: (field: string, value: string) => void;
  handleFileUpload: (field: string, file: File) => void;
  isAudio?: boolean;
  isUploading?: boolean;
}

/**
 * Component to display drag-and-drop or file upload controls with inline asset previews.
 * Resolves local Object URLs instead of large memory-bloating Base64 strings.
 */
export function MediaUploadField({
  field,
  inputs,
  isLocked,
  handleInputChange,
  handleFileUpload,
  isAudio: isAudioProp,
  isUploading = false,
}: Readonly<MediaUploadFieldProps>) {
  const { t } = useTranslation();
  const isAudio = isAudioProp ?? field.toLowerCase().includes("audio");
  const [previewUrl, setPreviewUrl] = useState("");
  const [fileName, setFileName] = useState("");
  const [isDragActive, setIsDragActive] = useState(false);

  // Revoke object URL on cleanup to prevent memory leaks
  useEffect(() => {
    return () => {
      if (previewUrl?.startsWith("blob:")) {
        URL.revokeObjectURL(previewUrl);
      }
    };
  }, [previewUrl]);

  // Synchronize initial value if reset by parent
  const currentValue = inputs[field] || "";
  const prevValueRef = useRef(currentValue);

  useEffect(() => {
    if (prevValueRef.current && !currentValue) {
      if (previewUrl?.startsWith("blob:")) {
        URL.revokeObjectURL(previewUrl);
      }
      setTimeout(() => {
        setPreviewUrl("");
        setFileName("");
      }, 0);
    }
    prevValueRef.current = currentValue;
  }, [currentValue, previewUrl]);

  const onFileChange = (file: File) => {
    const objectUrl = URL.createObjectURL(file);
    setPreviewUrl(objectUrl);
    setFileName(file.name);
    handleFileUpload(field, file);
  };

  const handleRemove = () => {
    if (previewUrl?.startsWith("blob:")) {
      URL.revokeObjectURL(previewUrl);
    }
    setPreviewUrl("");
    setFileName("");
    handleInputChange(field, "");
  };

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setIsDragActive(true);
    } else if (e.type === "dragleave") {
      setIsDragActive(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragActive(false);
    if (isLocked) return;

    if (e.dataTransfer.files?.[0]) {
      const file = e.dataTransfer.files[0];
      if (isAudio) {
        const fileExt = file.name.split(".").pop()?.toLowerCase();
        const supported = ["mp3", "wav", "aac"];
        if (
          !file.type.startsWith("audio/") &&
          (!fileExt || !supported.includes(fileExt))
        ) {
          alert(t.playground.invalidAudioFile);
          return;
        }
      } else if (!file.type.startsWith("image/")) {
        alert(t.playground.invalidImageFile);
        return;
      }
      onFileChange(file);
    }
  };

  return (
    <div className="flex flex-col gap-2">
      <span className="text-xs font-semibold text-zinc-500 uppercase tracking-wider">
        {t.playground.mediaPayload} ({field})
      </span>
      <div
        role="region"
        aria-label={t.playground.mediaPayload}
        onDragEnter={handleDrag}
        onDragOver={handleDrag}
        onDragLeave={handleDrag}
        onDrop={handleDrop}
        className={`border-2 border-dashed rounded-xl p-4 flex flex-col items-center justify-center min-h-[140px] transition-colors ${
          isDragActive
            ? "border-amber-500 bg-amber-500/5 dark:bg-amber-500/5"
            : "border-zinc-200 dark:border-zinc-800 bg-zinc-50/30 dark:bg-zinc-950/10"
        }`}
      >
        {isUploading ? (
          <div className="flex flex-col items-center gap-2">
            <Loader2 className="h-8 w-8 text-amber-500 animate-spin" />
            <span className="text-xs text-zinc-500 font-medium animate-pulse">
              {t.playground.uploadingAsset}
            </span>
          </div>
        ) : previewUrl ? (
          <div className="flex flex-col items-center gap-2 w-full">
            {fileName && (
              <span className="text-xs font-semibold text-zinc-600 dark:text-zinc-400 break-all px-2 text-center">
                {fileName}
              </span>
            )}
            {isAudio ? (
              <audio src={previewUrl} controls className="w-full max-h-12">
                <track kind="captions" />
              </audio>
            ) : (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={previewUrl}
                alt="Preview"
                className="max-h-32 rounded-lg object-contain"
              />
            )}
            <button
              type="button"
              onClick={handleRemove}
              className="text-xs text-red-500 hover:underline"
              disabled={isLocked}
            >
              {t.playground.removeAsset}
            </button>
          </div>
        ) : (
          <div className="flex flex-col items-center gap-2">
            <Upload className="h-8 w-8 text-zinc-400 animate-pulse" />
            <p className="text-xs text-zinc-500 text-center">
              {t.playground.dragAndDropAssetOr}
              <label
                className={`text-amber-500 ${isLocked ? "pointer-events-none opacity-50" : "cursor-pointer"} hover:underline`}
              >
                {t.playground.browse}
                <input
                  type="file"
                  disabled={isLocked}
                  onChange={(e) =>
                    e.target.files?.[0] && onFileChange(e.target.files[0])
                  }
                  accept={isAudio ? ".mp3,.wav,.aac,audio/*" : "image/*"}
                  className="hidden"
                />
              </label>
            </p>
            {isAudio && (
              <p className="text-[10px] text-zinc-400 text-center mt-1 font-medium">
                {t.playground.supportedFormatsAudio}
              </p>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
