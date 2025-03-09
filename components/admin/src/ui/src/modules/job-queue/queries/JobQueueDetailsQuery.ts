import { UseQueryOptions } from "@tanstack/react-query"
import { z } from "zod"
import { JobQueueDetails } from "../models/JobQueueDetails"

export const JobQueueDetailsQuery: (queueId: string) => UseQueryOptions<JobQueueDetails | null> = (queueId) => ({
  queryKey: ["job-queue", "queues", queueId, "details"],
  queryFn: async () => {
    // TODO handle 404
    return detailsSchema.parse(await (await fetch(`/admin/api/job-queue/queues/${queueId}/details`)).json())
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
