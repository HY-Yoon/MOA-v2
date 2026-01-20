// contexts/AlertContext.tsx
'use client';

import React, { createContext, useContext, useState, useCallback } from 'react';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/atoms/alert-dialog';

export interface AlertOptions {
  title: string;
  description?: string;
  confirmText?: string;
  cancelText?: string;
  variant?: 'default' | 'destructive';
}

interface AlertContextType {
  confirm: (options: AlertOptions) => Promise<boolean>;
  alert: (options: Omit<AlertOptions, 'cancelText'>) => Promise<void>;
}

const AlertContext = createContext<AlertContextType | undefined>(undefined);

export function AlertProvider({ children }: { children: React.ReactNode }) {
  const [isOpen, setIsOpen] = useState(false);
  const [config, setConfig] = useState<AlertOptions>({
    title: '',
    description: '',
    confirmText: '확인',
    cancelText: '취소',
    variant: 'default',
  });
  const [resolver, setResolver] = useState<((value: boolean) => void) | null>(null);

  const confirm = useCallback((options: AlertOptions): Promise<boolean> => {
    return new Promise((resolve) => {
      setConfig({
        confirmText: '확인',
        cancelText: '취소',
        variant: 'default',
        ...options,
      });
      setResolver(() => resolve);
      setIsOpen(true);
    });
  }, []);

  const alert = useCallback((options: Omit<AlertOptions, 'cancelText'>): Promise<void> => {
    return new Promise((resolve) => {
      setConfig({
        confirmText: '확인',
        variant: 'default',
        ...options,
        cancelText: undefined,
      });
      setResolver(() => resolve);
      setIsOpen(true);
    });
  }, []);

  const handleConfirm = () => {
    resolver?.(true);
    setIsOpen(false);
    setResolver(null);
  };

  const handleCancel = () => {
    resolver?.(false);
    setIsOpen(false);
    setResolver(null);
  };

  return (
    <AlertContext.Provider value={{ confirm, alert }}>
      {children}
      <AlertDialog open={isOpen} onOpenChange={setIsOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{config.title}</AlertDialogTitle>
            {config.description && (
              <AlertDialogDescription>{config.description}</AlertDialogDescription>
            )}
          </AlertDialogHeader>
          <AlertDialogFooter>
            {config.cancelText && (
              <AlertDialogCancel onClick={handleCancel}>{config.cancelText}</AlertDialogCancel>
            )}
            <AlertDialogAction
              onClick={handleConfirm}
              className={
                config.variant === 'destructive'
                  ? 'bg-destructive text-destructive-foreground hover:bg-destructive/90'
                  : ''
              }
            >
              {config.confirmText}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </AlertContext.Provider>
  );
}

export function useAlert() {
  const context = useContext(AlertContext);
  if (!context) {
    throw new Error('useAlert must be used within AlertProvider');
  }
  return context;
}
