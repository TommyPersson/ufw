import { JobQueueIndexPage } from "./ui/pages/JobQueueIndexPage.tsx"
import { ModuleDefinition } from "../ModuleDefinition.tsx"

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
        }
      ]
    }
  ]
}