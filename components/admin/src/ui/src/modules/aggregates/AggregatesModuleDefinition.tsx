import TextSnippetOutlinedIcon from '@mui/icons-material/TextSnippetOutlined';
import { ModuleDefinition } from "../ModuleDefinition.tsx"
import { AggregatesIndexPage } from "./ui/pages/AggregatesIndexPage"

export const AggregatesModuleDefinition: ModuleDefinition = {
  moduleId: "aggregates",
  navItems: [{
    title: "Aggregates",
    route: "aggregates",
    icon: <TextSnippetOutlinedIcon />,
  }],
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
