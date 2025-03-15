import { JobListPage } from "./ui/pages/JobListPage"
import { JobQueueIndexPage } from "./ui/pages/JobQueueIndexPage.tsx"
import { ModuleDefinition } from "../ModuleDefinition.tsx"
import { JobQueueDetailsPage } from "./ui/pages/JobQueueDetailsPage"

export const DurableJobsModuleDefinition: ModuleDefinition = {
  moduleId: "durable-jobs",
  navItemTitle: "Durable Jobs",
  indexRoute: "durable-jobs",
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
        }
      ]
    }
  ]
}
