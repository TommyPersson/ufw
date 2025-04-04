import { z } from "zod"
import { applicationModuleSchema } from "../../../common/models"
import { zx } from "../../../common/utils/zod"
import { jobQueueStateSchema } from "./JobQueueState"

export const periodicJobListItemSchema = z.object({
  type: z.string(),
  description: z.string().nullable(),
  cronExpression: z.string(),
  cronDescription: z.string(),
  lastSchedulingAttempt: zx.dateTime.nullable(),
  nextSchedulingAttempt: zx.dateTime.nullable(),
  queueId: z.string(),
  queueState: jobQueueStateSchema,
  queueHasFailures: z.boolean(),
  applicationModule: applicationModuleSchema,
})

export type PeriodicJobListItem = z.infer<typeof periodicJobListItemSchema>