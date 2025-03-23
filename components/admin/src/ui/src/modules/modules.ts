import { DurableCachesModuleDefinition } from "./durable-caches/DurableCachesModuleDefinition"
import { DurableJobsModuleDefinition } from "./durable-jobs/DurableJobsModuleDefinition.tsx"
import { FeatureTogglesModuleDefinition } from "./feature-toggles/FeatureTogglesModuleDefinition"
import { ModuleDefinition } from "./ModuleDefinition.tsx"

export const allModuleDefinitions: ModuleDefinition[] = [
  DurableJobsModuleDefinition,
  DurableCachesModuleDefinition,
  FeatureTogglesModuleDefinition,
]