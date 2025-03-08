import { UseQueryOptions } from "@tanstack/react-query"
import { z } from "zod"
import { JobQueueListItem } from "../models/JobQueueListItem"

export const JobQueueListQuery: UseQueryOptions<JobQueueListItem[]> = {
  queryKey: ["job-queue", "queues"],
  queryFn: async () => {
    return responseSchema.parse(await (await fetch("/admin/api/job-queue/queues")).json())
  },
}

const itemSchema = z.object({
  queueId: z.string()
})

const responseSchema = z.array(
  itemSchema
)