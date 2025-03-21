import { UseQueryOptions } from "@tanstack/react-query"
import { z } from "zod"
import { makeApiRequest } from "../../../common/utils/api"
import { JobQueueListItem, jobQueueListItemSchema } from "../models"

export const JobQueueListQuery: UseQueryOptions<JobQueueListItem[]> = {
  queryKey: ["durable-jobs", "queues"],
  queryFn: async () => {
    return responseSchema.parse(await makeApiRequest("/admin/api/durable-jobs/queues"))
  },
}


const responseSchema = z.array(
  jobQueueListItemSchema
)