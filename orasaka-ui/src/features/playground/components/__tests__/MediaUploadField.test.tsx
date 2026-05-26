import "@testing-library/jest-dom";
import { render, screen, fireEvent } from "@testing-library/react";
import { MediaUploadField } from "@/features/playground/components/MediaUploadField";

jest.mock("@/core/context/LocaleContext", () => ({
  useTranslation: () => ({
    t: {
      playground: {
        mediaPayload: "Media Payload",
        uploadingAsset: "Uploading...",
        removeAsset: "Remove",
        dragAndDropAssetOr: "Drag and drop or ",
        browse: "Browse",
        supportedFormatsAudio: "MP3, WAV, AAC",
        invalidImageFile: "Invalid image file",
        invalidAudioFile: "Invalid audio file",
      },
    },
    locale: "en",
  }),
}));

// Mock URL APIs
const mockRevokeURL = jest.fn();
const mockCreateURL = jest.fn(() => "blob:mock-url");
global.URL.createObjectURL = mockCreateURL;
global.URL.revokeObjectURL = mockRevokeURL;

describe("MediaUploadField", () => {
  const defaultProps = {
    field: "image",
    inputs: {},
    isLocked: false,
    handleInputChange: jest.fn(),
    handleFileUpload: jest.fn(),
  };

  afterEach(() => {
    jest.clearAllMocks();
  });

  it("renders field label", () => {
    render(<MediaUploadField {...defaultProps} />);
    expect(screen.getByText(/Media Payload/)).toBeInTheDocument();
    expect(screen.getByText(/\(image\)/)).toBeInTheDocument();
  });

  it("renders drop zone with browse button", () => {
    render(<MediaUploadField {...defaultProps} />);
    expect(screen.getByText("Browse")).toBeInTheDocument();
  });

  it("shows uploading state", () => {
    render(<MediaUploadField {...defaultProps} isUploading={true} />);
    expect(screen.getByText("Uploading...")).toBeInTheDocument();
  });

  it("shows supported audio formats hint when isAudio", () => {
    render(<MediaUploadField {...defaultProps} isAudio={true} />);
    expect(screen.getByText("MP3, WAV, AAC")).toBeInTheDocument();
  });

  it("handles file input change for image", () => {
    render(<MediaUploadField {...defaultProps} />);
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    const file = new File(["data"], "photo.png", { type: "image/png" });
    fireEvent.change(input, { target: { files: [file] } });
    expect(defaultProps.handleFileUpload).toHaveBeenCalledWith("image", file);
  });

  it("renders with locked state (file input disabled)", () => {
    render(<MediaUploadField {...defaultProps} isLocked={true} />);
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    expect(input).toBeDisabled();
  });

  it("renders remove button after file is selected", () => {
    render(<MediaUploadField {...defaultProps} />);
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    const file = new File(["data"], "test.png", { type: "image/png" });
    fireEvent.change(input, { target: { files: [file] } });
    expect(screen.getByText("Remove")).toBeInTheDocument();
  });

  it("calls handleInputChange on remove", () => {
    render(<MediaUploadField {...defaultProps} />);
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    const file = new File(["data"], "test.png", { type: "image/png" });
    fireEvent.change(input, { target: { files: [file] } });
    fireEvent.click(screen.getByText("Remove"));
    expect(defaultProps.handleInputChange).toHaveBeenCalledWith("image", "");
  });

  it("renders drop zone region with aria-label", () => {
    render(<MediaUploadField {...defaultProps} />);
    expect(screen.getByRole("region", { name: "Media Payload" })).toBeInTheDocument();
  });

  it("infers audio from field name", () => {
    render(<MediaUploadField {...defaultProps} field="audio_input" />);
    expect(screen.getByText("MP3, WAV, AAC")).toBeInTheDocument();
  });

  it("accepts audio/* for audio fields", () => {
    render(<MediaUploadField {...defaultProps} isAudio={true} />);
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    expect(input.accept).toContain("audio/*");
  });

  it("accepts image/* for non-audio fields", () => {
    render(<MediaUploadField {...defaultProps} isAudio={false} />);
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    expect(input.accept).toBe("image/*");
  });
});
