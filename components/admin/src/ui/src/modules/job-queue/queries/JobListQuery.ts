import { UseQueryOptions } from "@tanstack/react-query"
import { z } from "zod"
import { makeApiRequest } from "../../../common/utils/api"
import { zx } from "../../../common/utils/zod"
import { JobListItem } from "../models/JobListItem"
import { JobState } from "../models/JobState"

export const JobListQuery: (queueId: string, jobState: JobState, page: number) => UseQueryOptions<JobListItem[]> =
  (queueId, jobState, page) => ({
    queryKey: ["job-queue", "queues", queueId, "jobs", jobState, page],
    queryFn: async () => {
      return responseSchema.parse(await makeApiRequest(`/admin/api/job-queue/queues/${queueId}/jobs?state=${jobState}`))
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

const responseSchema = z.array(
  itemSchema
)