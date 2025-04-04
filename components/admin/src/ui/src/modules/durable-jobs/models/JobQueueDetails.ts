import { z } from "zod"
import { applicationModuleSchema } from "../../../common/models"
import { jobQueueStatusSchema } from "./JobQueueStatus"


export const jobQueueDetailsSchema = z.object({
  queueId: z.string(),
  numScheduled: z.number(),
  numPending: z.number(),
  numInProgress: z.number(),
  numFailed: z.number(),
  status: jobQueueStatusSchema,
  jobTypes: z.object({
    typeName: z.string(),
    className: z.string(),
    description: z.string().nullable(),
    periodic: z.boolean()
  }).array(),
  applicationModule: applicationModuleSchema,
})

export type JobQueueDetails = z.infer<typeof jobQueueDetailsSchema>