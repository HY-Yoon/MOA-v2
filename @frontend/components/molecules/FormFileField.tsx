'use client';

import { Button } from '@/components/atoms';
import { Upload, X } from 'lucide-react';
import { ChangeEvent, useEffect, useMemo, useRef, useState } from 'react';
import Image from 'next/image';
import { FormField } from './FormField';
import { getAbsoluteImageUrls } from '@/lib/common/image-url';

interface FormFileFieldProps {
  isLoading?: boolean;
  label: string;
  htmlFor: string;
  required?: boolean;
  error?: string;
  accept?: string;
  multiple?: boolean;
  maxSize?: number; // MB
  description?: string;
  onFileChange: (files: File[]) => void;
  previewImages?: string[] | string; // 단일 문자열 또는 배열
}

export function FormFileField({
  isLoading,
  label,
  htmlFor,
  required = false,
  error,
  accept = 'image/*',
  multiple = false,
  maxSize = 10,
  description,
  onFileChange,
  previewImages = [],
}: FormFileFieldProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [files, setFiles] = useState<File[]>([]);
  const [previews, setPreviews] = useState<string[]>([]);
  const onFileChangeRef = useRef(onFileChange);

  // 최신 onFileChange 함수 참조 유지
  useEffect(() => {
    onFileChangeRef.current = onFileChange;
  }, [onFileChange]);

  // previewImages 절대 경로 변환
  const absoluteImageUrls = useMemo(() => {
    if (!previewImages) return [];
    return getAbsoluteImageUrls(previewImages);
  }, [previewImages]);

  // 수정 화면인 경우 previewImages prop previews 상태 업데이트
  useEffect(() => {
    // 사용자가 새 파일을 첨부한 경우 previewImages 무시
    if (files.length > 0) return;

    // absoluteImageUrls가 변경되었을 때만 업데이트
    setPreviews((prevPreviews) => {
      const currentUrls = JSON.stringify(absoluteImageUrls);
      const previousUrls = JSON.stringify(prevPreviews);

      // 값이 같으면 이전 상태 반환 (불필요한 리렌더링 방지)
      return currentUrls === previousUrls ? prevPreviews : absoluteImageUrls;
    });
  }, [absoluteImageUrls, files.length]);

  // 파일이 변경될 때 부모에게 알림
  useEffect(() => {
    onFileChangeRef.current(files);
  }, [files]);

  const handleFileChange = (e: ChangeEvent<HTMLInputElement>) => {
    const selectedFiles = e.target.files;
    if (selectedFiles && selectedFiles.length > 0) {
      const fileArray = Array.from(selectedFiles);

      // 파일 크기 검증
      const invalidFiles: string[] = [];
      fileArray.forEach((file) => {
        if (file.size > maxSize * 1024 * 1024) {
          invalidFiles.push(file.name);
        }
      });

      if (invalidFiles.length > 0) {
        alert(`파일 크기가 ${maxSize}MB를 초과합니다: ${invalidFiles.join(', ')}`);
        return;
      }

      if (multiple) {
        // 다중 파일: 기존 파일에 추가
        setFiles((prev) => [...prev, ...fileArray]);
        fileArray.forEach((file) => {
          const reader = new FileReader();
          reader.onloadend = () => {
            setPreviews((prev) => [...prev, reader.result as string]);
          };
          reader.readAsDataURL(file);
        });
      } else {
        // 단일 파일: 교체
        setFiles([fileArray[0]]);
        const reader = new FileReader();
        reader.onloadend = () => {
          setPreviews([reader.result as string]);
        };
        reader.readAsDataURL(fileArray[0]);
      }
    }

    // 같은 파일을 다시 선택할 수 있도록 input 초기화
    if (e.target) {
      e.target.value = '';
    }
  };

  const handleClick = () => {
    fileInputRef.current?.click();
  };

  const handleRemove = (index: number) => {
    setFiles((prev) => prev.filter((_, i) => i !== index));
    setPreviews((prev) => prev.filter((_, i) => i !== index));
  };

  return (
    <FormField
      isLoading={isLoading}
      label={label}
      htmlFor={htmlFor}
      required={required}
      error={error}
      description={description}
    >
      <div className="space-y-4">
        <div className="rounded-lg border-2 border-dashed border-slate-300 p-8 text-center transition-colors hover:border-slate-400">
          {previews.length > 0 ? (
            <div className="space-y-4">
              {multiple ? (
                <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
                  {previews.map((preview, index) => (
                    <div
                      key={index}
                      className="group relative aspect-square overflow-hidden rounded-lg"
                    >
                      <Image
                        src={preview}
                        alt={`미리보기 ${index + 1}`}
                        fill
                        className="object-cover shadow-md"
                        unoptimized
                      />
                      <button
                        type="button"
                        onClick={() => handleRemove(index)}
                        className="absolute top-1 right-1 z-10 rounded-full bg-red-500 p-1 text-white opacity-0 transition-opacity group-hover:opacity-100"
                      >
                        <X className="h-4 w-4" />
                      </button>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="space-y-6">
                  <div className="relative mx-auto max-h-96 w-full max-w-md overflow-hidden rounded-lg">
                    <Image
                      src={previews[0]}
                      alt="미리보기"
                      width={400}
                      height={400}
                      className="h-auto max-h-96 w-full object-contain shadow-md"
                      unoptimized
                    />
                  </div>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => handleRemove(0)}
                    className="mx-auto"
                  >
                    <X className="mr-2 h-4 w-4" />
                    이미지 제거
                  </Button>
                </div>
              )}
              <Button type="button" variant="outline" onClick={handleClick}>
                <Upload className="mr-2 h-4 w-4" />
                {multiple ? '이미지 추가' : '이미지 변경'}
              </Button>
            </div>
          ) : (
            <label htmlFor={htmlFor} className="block cursor-pointer">
              <Upload className="mx-auto mb-4 h-12 w-12 text-slate-400" />
              <p className="mb-2 text-sm text-slate-600">클릭하여 이미지를 업로드하세요</p>
              <p className="text-xs text-slate-400">
                JPG, JPEG, PNG, GIF, WEBP 파일 (최대 {maxSize}MB
                {multiple ? ', 여러 개 선택 가능' : ''})
              </p>
              <input
                ref={fileInputRef}
                id={htmlFor}
                type="file"
                accept={accept}
                multiple={multiple}
                onChange={handleFileChange}
                className="hidden"
              />
            </label>
          )}
        </div>
        {previews.length > 0 && (
          <input
            ref={fileInputRef}
            id={`${htmlFor}-hidden`}
            type="file"
            accept={accept}
            multiple={multiple}
            onChange={handleFileChange}
            className="hidden"
          />
        )}
      </div>
    </FormField>
  );
}
