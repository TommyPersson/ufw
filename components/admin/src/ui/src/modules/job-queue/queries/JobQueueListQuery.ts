import { UseQueryOptions } from "@tanstack/react-query"
import { z } from "zod"
import { delay } from "../../../common/utils/async"
import { JobQueueListItem } from "../models/JobQueueListItem"

export const JobQueueListQuery: UseQueryOptions<JobQueueListItem[]> = {
  queryKey: ["job-queue", "queues"],
  queryFn: async () => {
    await delay(1000)
    return responseSchema.parse(await (await fetch("/admin/api/job-queue/queues")).json())
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