import { cn } from '@/lib/utils';

interface SkeletonProps {
  className?: string;
}

export function Skeleton({ className }: SkeletonProps) {
  return (
    <div
      className={cn(
        'animate-pulse rounded-lg bg-muted',
        className
      )}
    />
  );
}

export function BalanceCardSkeleton() {
  return (
    <div className="balance-card">
      <Skeleton className="h-4 w-24 bg-white/20 mb-2" />
      <Skeleton className="h-10 w-48 bg-white/20 mb-4" />
      <Skeleton className="h-3 w-32 bg-white/20" />
    </div>
  );
}

export function TransactionItemSkeleton() {
  return (
    <div className="transaction-item">
      <Skeleton className="w-12 h-12 rounded-full" />
      <div className="flex-1">
        <Skeleton className="h-4 w-32 mb-2" />
        <Skeleton className="h-3 w-24" />
      </div>
      <Skeleton className="h-5 w-20" />
    </div>
  );
}

export function TransactionListSkeleton({ count = 5 }: { count?: number }) {
  return (
    <div className="space-y-2">
      {Array.from({ length: count }).map((_, i) => (
        <TransactionItemSkeleton key={i} />
      ))}
    </div>
  );
}

export function ProfileSkeleton() {
  return (
    <div className="flex flex-col items-center">
      <Skeleton className="w-24 h-24 rounded-full mb-4" />
      <Skeleton className="h-6 w-32 mb-2" />
      <Skeleton className="h-4 w-40" />
    </div>
  );
}

export function QuickActionsSkeleton() {
  return (
    <div className="grid grid-cols-4 gap-3">
      {Array.from({ length: 4 }).map((_, i) => (
        <div key={i} className="flex flex-col items-center gap-2">
          <Skeleton className="w-14 h-14 rounded-2xl" />
          <Skeleton className="h-3 w-12" />
        </div>
      ))}
    </div>
  );
}
