import { AdminRequestsModuleDefinition } from "./admin-requests/AdminRequestsModuleDefinition"
import { AggregatesModuleDefinition } from "./aggregates/AggregatesModuleDefinition"
import { ClusterModuleDefinition } from "./cluster/ClusterModuleDefinition"
import { DurableCachesModuleDefinition } from "./durable-caches/DurableCachesModuleDefinition"
import { DurableMessagesModuleDefinition } from "./durable-messages/DurableMessagesModuleDefinition"
import { DurableJobsModuleDefinition } from "./durable-jobs/DurableJobsModuleDefinition"
import { FeatureTogglesModuleDefinition } from "./feature-toggles/FeatureTogglesModuleDefinition"
import { ModuleDefinition } from "./ModuleDefinition.tsx"

export const allModuleDefinitions: ModuleDefinition[] = [
  ClusterModuleDefinition,
  AdminRequestsModuleDefinition,
  AggregatesModuleDefinition,
  DurableJobsModuleDefinition,
  DurableMessagesModuleDefinition,
  DurableCachesModuleDefinition,
  FeatureTogglesModuleDefinition,
]