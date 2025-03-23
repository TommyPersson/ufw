import { ModuleDefinition } from "../ModuleDefinition.tsx"
import { DurableCacheDetailsPage } from "./ui/pages/DurableCacheDetailsPage"
import { DurableCachesIndexPage } from "./ui/pages/DurableCachesIndexPage.tsx"

export const DurableCachesModuleDefinition: ModuleDefinition = {
  moduleId: "durable-caches",
  navItemTitle: "Durable Caches",
  indexRoute: "durable-caches",
  routes: [
    {
      path: "durable-caches",
      children: [
        {
          index: true,
          Component: DurableCachesIndexPage,
        },
        {
          path: "caches/:cacheId",
          Component: DurableCacheDetailsPage,
        }
      ]
    }
  ]
}
