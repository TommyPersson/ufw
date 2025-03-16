import { UseQueryOptions } from "@tanstack/react-query"
import { z } from "zod"
import { makeApiRequest } from "../../../common/utils/api"
import { zx } from "../../../common/utils/zod"
import { JobDetails, jobStateSchema } from "../models"

export const JobDetailsQuery: (queueId: string, jobId: string) => UseQueryOptions<JobDetails | null> = (queueId, jobId) => ({
  queryKey: ["durable-jobs", "queues", queueId, "job", jobId, "details"],
  queryFn: async () => {
    return detailsSchema.parse(await makeApiRequest(`/admin/api/durable-jobs/queues/${queueId}/jobs/${jobId}/details`))
  },
})

const detailsSchema = z.object({
  jobId: z.string(),
  queueId: z.string(),
  jobType: z.string(),
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
