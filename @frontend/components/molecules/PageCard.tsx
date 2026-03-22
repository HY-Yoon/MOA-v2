'use client';

import { ArrowLeft } from 'lucide-react';
import { Button, Card, CardContent, CardHeader, CardTitle } from '@/components/atoms';
import { useRouter } from 'next/navigation';
import { ReactNode, Children, isValidElement, cloneElement } from 'react';

interface PageCardProps {
  children?: ReactNode;
}

function PageCardTitle({
  children,
  buttonGroups,
  useRouteBack = true,
}: {
  children?: ReactNode;
  buttonGroups?: ReactNode;
  useRouteBack?: boolean;
}) {
  const router = useRouter();

  const handleRouterBack = () => {
    router.back();
  };

  return (
    <CardHeader>
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          {useRouteBack && (
            <Button type="button" variant="ghost" size="icon" onClick={handleRouterBack}>
              <ArrowLeft className="size-6" />
            </Button>
          )}
          <CardTitle className="text-2xl font-bold">{children}</CardTitle>
        </div>
        {buttonGroups && <div className="flex items-center">{buttonGroups}</div>}
      </div>
    </CardHeader>
  );
}

function PageCardContent({ children }: { children?: ReactNode }) {
  return <CardContent>{children}</CardContent>;
}

export function PageCard({ children }: PageCardProps) {
  const headerContent = Children.toArray(children).find(
    (child) => isValidElement(child) && child.type === PageCardTitle,
  );

  const contentContent = Children.toArray(children).find(
    (child) => isValidElement(child) && child.type === PageCardContent,
  );

  return (
    <Card>
      {headerContent && cloneElement(headerContent as React.ReactElement)}
      {contentContent && cloneElement(contentContent as React.ReactElement)}
    </Card>
  );
}
// Compound components
PageCard.Title = PageCardTitle;
PageCard.Content = PageCardContent;
