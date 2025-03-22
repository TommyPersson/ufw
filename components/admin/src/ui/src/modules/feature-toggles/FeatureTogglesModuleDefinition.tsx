import { ModuleDefinition } from "../ModuleDefinition.tsx"
import { FeatureTogglesIndexPage } from "./ui/pages/FeatureTogglesIndexPage.tsx"

export const FeatureTogglesModuleDefinition: ModuleDefinition = {
  moduleId: "feature-toggles",
  navItemTitle: "Feature Toggles",
  indexRoute: "feature-toggles",
  routes: [
    {
      path: "feature-toggles",
      children: [
        {
          index: true,
          Component: FeatureTogglesIndexPage,
        },
      ]
    }
  ]
}
