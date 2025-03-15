import { UseQueryOptions } from "@tanstack/react-query"
import { z } from "zod"
import { makeApiRequest } from "../../../common/utils/api"
import { JobQueueListItem } from "../models"

export const JobQueueListQuery: UseQueryOptions<JobQueueListItem[]> = {
  queryKey: ["durable-jobs", "queues"],
  queryFn: async () => {
    return responseSchema.parse(await makeApiRequest("/admin/api/durable-jobs/queues"))
  },
}

const itemSchema = z.object({
  queueId: z.string(),
  numScheduled: z.number(),
  numPending: z.number(),
  numInProgress: z.number(),
  numFailed: z.number(),
})

const responseSchema = z.array(
  itemSchema
)