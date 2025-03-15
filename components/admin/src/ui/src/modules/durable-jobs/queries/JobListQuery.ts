import { UseQueryOptions } from "@tanstack/react-query"
import { z } from "zod"
import { makeApiRequest, PaginatedList } from "../../../common/utils/api"
import { zx } from "../../../common/utils/zod"
import { JobListItem } from "../models/JobListItem"
import { JobState } from "../models/JobState"

export const JobListQuery: (queueId: string, jobState: JobState, page: number) => UseQueryOptions<PaginatedList<JobListItem>> =
  (queueId, jobState, page) => ({
    queryKey: ["durable-jobs", "queues", queueId, "jobs", jobState, page],
    queryFn: async () => {
      const limit = 100
      const offset = (page - 1) * 100
      return responseSchema.parse(await makeApiRequest(`/admin/api/durable-jobs/queues/${queueId}/jobs?state=${jobState}&limit=${limit}&offset=${offset}`))
    },
  })

const itemSchema = z.object({
  jobId: z.string(),
  numFailures: z.number(),
  createdAt: zx.dateTime,
  firstScheduledFor: zx.dateTime,
  nextScheduledFor: zx.dateTime.nullable(),
  stateChangedAt: zx.dateTime,
})

const responseSchema = zx.paginatedList(
  itemSchema
)
