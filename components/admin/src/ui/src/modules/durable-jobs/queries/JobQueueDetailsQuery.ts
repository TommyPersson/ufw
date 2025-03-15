import { UseQueryOptions } from "@tanstack/react-query"
import { z } from "zod"
import { makeApiRequest } from "../../../common/utils/api"
import { JobQueueDetails } from "../models/JobQueueDetails"

export const JobQueueDetailsQuery: (queueId: string) => UseQueryOptions<JobQueueDetails | null> = (queueId) => ({
  queryKey: ["durable-jobs", "queues", queueId, "details"],
  queryFn: async () => {
    return detailsSchema.parse(await makeApiRequest(`/admin/api/durable-jobs/queues/${queueId}/details`))
  },
})

const detailsSchema = z.object({
  queueId: z.string(),
  numScheduled: z.number(),
  numPending: z.number(),
  numInProgress: z.number(),
  numFailed: z.number(),
  jobTypes: z.object({
    type: z.string(),
    jobClassName: z.string(),
    description: z.string().nullable(),
  }).array()
})
