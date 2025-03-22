import { FeatureTogglesModuleDefinition } from "./feature-toggles/FeatureTogglesModuleDefinition"
import { ModuleDefinition } from "./ModuleDefinition.tsx"
import { DurableJobsModuleDefinition } from "./durable-jobs/DurableJobsModuleDefinition.tsx"

export const allModuleDefinitions: ModuleDefinition[] = [
  DurableJobsModuleDefinition,
  FeatureTogglesModuleDefinition,
]