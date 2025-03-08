import { ModuleDefinition } from "./ModuleDefinition.tsx"
import { JobQueueModuleDefinition } from "./job-queue/JobQueueModuleDefinition.tsx"

export const allModuleDefinitions: ModuleDefinition[] = [
  JobQueueModuleDefinition,
]