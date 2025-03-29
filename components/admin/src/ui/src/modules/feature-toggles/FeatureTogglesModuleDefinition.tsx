import ToggleOnOutlinedIcon from "@mui/icons-material/ToggleOnOutlined"
import { ModuleDefinition } from "../ModuleDefinition.tsx"
import { FeatureTogglesIndexPage } from "./ui/pages/FeatureTogglesIndexPage.tsx"

export const FeatureTogglesModuleDefinition: ModuleDefinition = {
  moduleId: "feature-toggles",
  navItems: [{
    title: "Feature Toggles",
    route: "feature-toggles",
    icon: <ToggleOnOutlinedIcon />,
  }],
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
