import FormatListBulletedOutlinedIcon from "@mui/icons-material/FormatListBulletedOutlined"
import { ModuleDefinition } from "../ModuleDefinition.tsx"
import { JobDetailsPage } from "./ui/pages/JobDetailsPage"
import { JobListPage } from "./ui/pages/JobListPage"
import { JobQueueDetailsPage } from "./ui/pages/JobQueueDetailsPage"
import { JobQueueIndexPage } from "./ui/pages/JobQueueIndexPage.tsx"

export const DurableJobsModuleDefinition: ModuleDefinition = {
  moduleId: "durable-jobs",
  navItems: [{
    title: "Durable Jobs",
    route: "durable-jobs",
    icon: <FormatListBulletedOutlinedIcon />,
  }],
  routes: [
    {
      path: "durable-jobs",
      children: [
        {
          index: true,
          Component: JobQueueIndexPage,
        },
        {
          path: "queues/:queueId/details",
          Component: JobQueueDetailsPage,
        },
        {
          path: "queues/:queueId/jobs/:jobState",
          Component: JobListPage,
        },
        {
          path: "queues/:queueId/jobs/by-id/:jobId/details",
          Component: JobDetailsPage
        }
      ]
    }
  ]
}
