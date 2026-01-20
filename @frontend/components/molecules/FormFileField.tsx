'use client';

import { Button } from '@/components/atoms';
import { Upload, X } from 'lucide-react';
import { ChangeEvent, useEffect, useRef, useState } from 'react';
import Image from 'next/image';
import { FormField } from './FormField';

// 다중 파일 수정 타입 (ex. 상세 이미지)
type MultipleUpdate = { id: number; url: string };
// 파일 아이템 타입 (새 파일 또는 기존 URL)
export type FileItem =
  | { type: 'file'; file: File; preview: string }
  | ({ type: 'url' } & MultipleUpdate);

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
  onFileChange: (newFiles: File[], deletedIds: number[]) => void;
  // 단일 string, 다중 생성 string[], 다중 수정 MultipleUpdate[]
  previewImages?: string | string[] | MultipleUpdate[];
}

export function FormFileField({
  isLoading,
  label,
  htmlFor,
  required = false,
  error,
  multiple = false,
  maxSize = 10,
  description,
  onFileChange,
  previewImages = [],
}: FormFileFieldProps) {
  const FILE_TYPE = { FILE: 'file', URL: 'url' } as const;

  const fileInputRef = useRef<HTMLInputElement>(null);
  const onFileChangeRef = useRef(onFileChange);

  const [fileItems, setFileItems] = useState<FileItem[]>([]);
  const [deletedIds, setDeletedIds] = useState<number[]>([]);

  // 최신 onFileChange 함수 참조 유지
  useEffect(() => {
    onFileChangeRef.current = onFileChange;
  }, [onFileChange]);

  // previewImages 초기화
  useEffect(() => {
    if (!previewImages) return;

    // 1. {string} 단일 생성/수정 (포스터)
    if (typeof previewImages === 'string') {
      setFileItems([
        {
          type: FILE_TYPE.URL,
          id: 0, // ID 없는 경우 0으로 표시
          url: previewImages,
        },
      ]);
    } else if (Array.isArray(previewImages)) {
      // 배열인 경우 (상세 이미지)
      const urlItems: FileItem[] = previewImages.map((img) => {
        const isStringType = typeof img === 'string';
        // 2. {string[]} 다중 생성 / {MultipleUpdate[]} 다중 수정
        return {
          type: FILE_TYPE.URL,
          id: isStringType ? 0 : img.id,
          url: isStringType ? img : img.url,
        };
      });
      setFileItems(urlItems);
    }
  }, [previewImages]);

  const isFileItem = (item: FileItem): item is Extract<FileItem, { type: 'file' }> =>
    item.type === FILE_TYPE.FILE;
  const isUrlItem = (item: FileItem): item is Extract<FileItem, { type: 'url' }> =>
    item.type === FILE_TYPE.URL;

  // 파일 삭제 ID 변경 시 부모에게 알림
  useEffect(() => {
    const newFiles = fileItems.filter(isFileItem).map((item) => item.file);

    onFileChangeRef.current(newFiles, deletedIds);
  }, [fileItems, deletedIds]);

  const handleFileChange = (e: ChangeEvent<HTMLInputElement>) => {
    const selectedFiles = e.target.files;
    if (!selectedFiles || selectedFiles.length === 0) return;

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

    // 단일 파일: 기존 URL 삭제 처리
    if (!multiple) {
      setFileItems((prev) => {
        const urlItem = prev.find(isUrlItem);
        if (urlItem && urlItem.id > 0) {
          setDeletedIds((ids) => [...ids, urlItem.id]);
        }
        return [];
      });
    }

    // FileReader로 미리보기 생성
    fileArray.forEach((file) => {
      const reader = new FileReader();
      reader.onloadend = () => {
        const newItem: FileItem = {
          type: 'file',
          file,
          preview: reader.result as string,
        };

        if (multiple) {
          // 다중 파일: 추가
          setFileItems((prev) => [...prev, newItem]);
        } else {
          // 단일 파일: 교체
          setFileItems([newItem]);
        }
      };
      reader.readAsDataURL(file);
    });

    // input 초기화
    if (e.target) {
      e.target.value = '';
    }
  };

  const handleClick = () => {
    fileInputRef.current?.click();
  };

  const handleRemove = (index: number) => {
    const item = fileItems[index];

    // URL 타입이고 ID가 있으면 삭제 목록에 추가
    if (item.type === 'url' && item.id > 0) {
      setDeletedIds((prev) => [...prev, item.id]);
    }

    setFileItems((prev) => prev.filter((_, i) => i !== index));
  };

  // 미리보기 URL 추출
  const previews = fileItems.map((item) => (item.type === 'file' ? item.preview : item.url));

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
                accept="image/*"
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
            accept="image/*"
            multiple={multiple}
            onChange={handleFileChange}
            className="hidden"
          />
        )}
      </div>
    </FormField>
  );
}
