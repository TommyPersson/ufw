import { JobListPage } from "./ui/pages/JobListPage"
import { JobQueueIndexPage } from "./ui/pages/JobQueueIndexPage.tsx"
import { ModuleDefinition } from "../ModuleDefinition.tsx"
import { JobQueueDetailsPage } from "./ui/pages/JobQueueDetailsPage"

export const JobQueueModuleDefinition: ModuleDefinition = {
  moduleId: "job-queue",
  navItemTitle: "Job Queue",
  indexRoute: "job-queue",
  routes: [
    {
      path: "job-queue",
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
        }
      ]
    }
  ]
}
