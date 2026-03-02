import { ScrollToTopButton } from '@/components/molecules';
import ShowCatalogList from './_components/ShowCatalogList';

export default function ShowsPage() {
  return (
    <section className="container mx-auto px-4 py-10">
      <ShowCatalogList />
      <ScrollToTopButton />
    </section>
  );
}
