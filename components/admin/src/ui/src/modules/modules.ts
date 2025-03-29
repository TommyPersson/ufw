import { AdminRequestsModuleDefinition } from "./admin-requests/AdminRequestsModuleDefinition"
import { AggregatesModuleDefinition } from "./aggregates/AggregatesModuleDefinition"
import { DurableCachesModuleDefinition } from "./durable-caches/DurableCachesModuleDefinition"
import { DurableEventsModuleDefinition } from "./durable-events/DurableEventsModuleDefinition"
import { DurableJobsModuleDefinition } from "./durable-jobs/DurableJobsModuleDefinition"
import { FeatureTogglesModuleDefinition } from "./feature-toggles/FeatureTogglesModuleDefinition"
import { ModuleDefinition } from "./ModuleDefinition.tsx"

export const allModuleDefinitions: ModuleDefinition[] = [
  AdminRequestsModuleDefinition,
  AggregatesModuleDefinition,
  DurableJobsModuleDefinition,
  DurableEventsModuleDefinition,
  DurableCachesModuleDefinition,
  FeatureTogglesModuleDefinition,
]