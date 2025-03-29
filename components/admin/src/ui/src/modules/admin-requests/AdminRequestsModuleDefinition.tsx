import ManageSearchOutlinedIcon from '@mui/icons-material/ManageSearchOutlined';
import PlayCircleOutlineOutlinedIcon from '@mui/icons-material/PlayCircleOutlineOutlined';
import { ModuleDefinition } from "../ModuleDefinition.tsx"
import { AdminRequestsIndexPage } from "./ui/pages"

export const AdminRequestsModuleDefinition: ModuleDefinition = {
  moduleId: "admin-requests",
  navItems: [{
    title: "Admin Commands",
    route: 'admin-commands',
    icon: <PlayCircleOutlineOutlinedIcon />,
  }, {
    title: "Admin Queries",
    route: 'admin-queries',
    icon: <ManageSearchOutlinedIcon />,
  }],
  routes: [
    {
      path: "admin-commands",
      children: [
        {
          index: true,
          element: <AdminRequestsIndexPage requestType={"COMMAND"} />,
        },
      ]
    },
    {
      path: "admin-queries",
      children: [
        {
          index: true,
          element: <AdminRequestsIndexPage requestType={"QUERY"} />,
        },
      ]
    },
  ]
}
