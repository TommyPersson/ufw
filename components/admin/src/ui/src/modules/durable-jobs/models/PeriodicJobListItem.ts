import { z } from "zod"
import { applicationModuleSchema } from "../../../common/models"
import { zx } from "../../../common/utils/zod"
import { jobQueueStateSchema } from "./JobQueueState"
import { jobStateSchema } from "./JobState"

export const periodicJobListItemSchema = z.object({
  type: z.string(),
  description: z.string().nullable(),
  cronExpression: z.string(),
  cronDescription: z.string(),
  lastSchedulingAttempt: zx.dateTime.nullable(),
  nextSchedulingAttempt: zx.dateTime.nullable(),
  queueId: z.string(),
  queueState: jobQueueStateSchema,
  queueNumFailures: z.number(),
  lastExecutionState: jobStateSchema.nullable(),
  lastExecutionStateChangeTimestamp: zx.dateTime.nullable(),
  applicationModule: applicationModuleSchema,
})

export type PeriodicJobListItem = z.infer<typeof periodicJobListItemSchema>