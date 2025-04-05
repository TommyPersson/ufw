import { UseQueryOptions } from "@tanstack/react-query"
import { makeApiRequest } from "../../../common/utils/api"
import { JobDetails, jobDetailsSchema } from "../models"

export const JobDetailsQuery: (queueId: string, jobId: string) => UseQueryOptions<JobDetails | null> = (queueId, jobId) => ({
  queryKey: ["durable-jobs", "queues", queueId, "job", jobId, "details"],
  queryFn: async () => {
    return jobDetailsSchema.parse(await makeApiRequest(`/admin/api/durable-jobs/queues/${queueId}/jobs/${jobId}/details`))
  },
})
