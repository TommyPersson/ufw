import { UseQueryOptions } from "@tanstack/react-query"
import { makeApiRequest } from "../../../common/utils/api"
import { JobQueueDetails, jobQueueDetailsSchema } from "../models"

export const JobQueueDetailsQuery: (queueId: string) => UseQueryOptions<JobQueueDetails | null> = (queueId) => ({
  queryKey: ["durable-jobs", "queues", queueId, "details"],
  queryFn: async () => {
    return jobQueueDetailsSchema.parse(await makeApiRequest(`/admin/api/durable-jobs/queues/${queueId}/details`))
  },
})