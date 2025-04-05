import { z } from "zod"
import { zx } from "../../../common/utils/zod"
import { jobStateSchema } from "./JobState"

export const jobDetailsSchema = z.object({
  jobId: z.string(),
  queueId: z.string(),
  jobType: z.string(),
  jobTypeClass: z.string(),
  jobTypeDescription: z.string().nullable(),
  state: jobStateSchema,
  dataJson: z.string(),
  metadataJson: z.string(),
  concurrencyKey: z.string().nullable(),
  createdAt: zx.dateTime,
  firstScheduledFor: zx.dateTime,
  nextScheduledFor: zx.dateTime.nullable(),
  stateChangedAt: zx.dateTime,
  watchdogTimestamp: zx.dateTime.nullable(),
  watchdogOwner: z.string().nullable(),
  numFailures: z.number(),
  expiresAt: zx.dateTime.nullable(),
})

export type JobDetails = z.infer<typeof jobDetailsSchema>