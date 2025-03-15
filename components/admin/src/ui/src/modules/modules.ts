import { ModuleDefinition } from "./ModuleDefinition.tsx"
import { DurableJobsModuleDefinition } from "./durable-jobs/DurableJobsModuleDefinition.tsx"

export const allModuleDefinitions: ModuleDefinition[] = [
  DurableJobsModuleDefinition,
]