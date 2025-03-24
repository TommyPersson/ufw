import { ModuleDefinition } from "../ModuleDefinition.tsx"
import { AggregatesIndexPage } from "./ui/pages/AggregatesIndexPage"

export const AggregatesModuleDefinition: ModuleDefinition = {
  moduleId: "aggregates",
  navItemTitle: "Aggregates",
  indexRoute: "aggregates",
  routes: [
    {
      path: "aggregates",
      children: [
        {
          index: true,
          Component: AggregatesIndexPage,
        },
      ]
    }
  ]
}
