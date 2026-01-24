import React from 'react';
import { User } from 'lucide-react';
import { cn } from '@/lib/utils';

interface AvatarDisplayProps {
  src?: string;
  name?: string;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  className?: string;
}

const sizeClasses = {
  sm: 'w-8 h-8 text-xs',
  md: 'w-10 h-10 text-sm',
  lg: 'w-12 h-12 text-base',
  xl: 'w-20 h-20 text-xl',
};

function getInitials(name?: string): string {
  if (!name) return '';
  const parts = name.trim().split(' ');
  if (parts.length === 1) return parts[0].charAt(0).toUpperCase();
  return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
}

export function AvatarDisplay({
  src,
  name,
  size = 'md',
  className,
}: AvatarDisplayProps) {
  const initials = getInitials(name);

  return (
    <div
      className={cn(
        'rounded-full bg-primary/10 flex items-center justify-center overflow-hidden',
        sizeClasses[size],
        className
      )}
    >
      {src ? (
        <img
          src={src}
          alt={name || 'Avatar'}
          className="w-full h-full object-cover"
        />
      ) : initials ? (
        <span className="font-semibold text-primary">{initials}</span>
      ) : (
        <User className="w-1/2 h-1/2 text-muted-foreground" />
      )}
    </div>
  );
}
