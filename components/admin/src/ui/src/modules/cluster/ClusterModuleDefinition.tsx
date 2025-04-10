import StorageOutlinedIcon from "@mui/icons-material/StorageOutlined"
import { ModuleDefinition } from "../ModuleDefinition.tsx"
import { ClusterIndexPage } from "./ui/pages/ClusterIndexPage"

export const ClusterModuleDefinition: ModuleDefinition = {
  moduleId: "cluster",
  navItems: [{
    title: "Cluster Instances",
    route: "cluster",
    icon: <StorageOutlinedIcon />,
  }],
  routes: [
    {
      path: "cluster",
      children: [
        {
          index: true,
          Component: ClusterIndexPage,
        },
      ]
    }
  ]
}
