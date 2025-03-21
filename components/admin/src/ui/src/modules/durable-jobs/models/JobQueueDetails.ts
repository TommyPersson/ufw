import { z } from "zod"
import { jobQueueStatusSchema } from "./JobQueueStatus"


export const jobQueueDetailsSchema = z.object({
  queueId: z.string(),
  numScheduled: z.number(),
  numPending: z.number(),
  numInProgress: z.number(),
  numFailed: z.number(),
  status: jobQueueStatusSchema,
  jobTypes: z.object({
    type: z.string(),
    jobClassName: z.string(),
    description: z.string().nullable(),
  }).array(),
})

export type JobQueueDetails = z.infer<typeof jobQueueDetailsSchema>