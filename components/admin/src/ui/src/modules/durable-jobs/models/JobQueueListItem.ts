import { z } from "zod"
import { applicationModuleSchema } from "../../../common/models"
import { jobQueueStatusSchema } from "./JobQueueStatus"

export const jobQueueListItemSchema = z.object({
  queueId: z.string(),
  numScheduled: z.number(),
  numPending: z.number(),
  numInProgress: z.number(),
  numFailed: z.number(),
  status: jobQueueStatusSchema,
  hasOnlyPeriodicJobTypes: z.boolean(),
  applicationModule: applicationModuleSchema,
})

export type JobQueueListItem = z.infer<typeof jobQueueListItemSchema>