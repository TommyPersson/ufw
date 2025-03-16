import { UseQueryOptions } from "@tanstack/react-query"
import { z } from "zod"
import { makeApiRequest, PaginatedList } from "../../../common/utils/api"
import { zx } from "../../../common/utils/zod"
import { JobFailure } from "../models/JobFailure"

export const JobFailuresQuery: (queueId: string, jobId: string) => UseQueryOptions<PaginatedList<JobFailure>> =
  (queueId, jobId) => ({
    queryKey: ["durable-jobs", "queues", queueId, "job", jobId, "failures"],
    queryFn: async () => {
      return responseSchema.parse(await makeApiRequest(`/admin/api/durable-jobs/queues/${queueId}/jobs/${jobId}/failures?limit=5`))
    },
  })

const itemSchema = z.object({
  failureId: z.string(),
  jobId: z.string(),
  timestamp: zx.dateTime,
  errorType: z.string(),
  errorMessage: z.string(),
  errorStackTrace: z.string(),
})

const responseSchema = zx.paginatedList(
  itemSchema
)
