import { z } from "zod"
import { jobQueueStatusSchema } from "./JobQueueStatus"

export const jobQueueListItemSchema = z.object({
  queueId: z.string(),
  numScheduled: z.number(),
  numPending: z.number(),
  numInProgress: z.number(),
  numFailed: z.number(),
  status: jobQueueStatusSchema,
})

export type JobQueueListItem = z.infer<typeof jobQueueListItemSchema>