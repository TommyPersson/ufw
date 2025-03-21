import { z } from "zod"
import { zx } from "../../../common/utils/zod"
import { jobQueueStateSchema } from "./JobQueueState"

export const jobQueueStatusSchema = z.object({
  state: jobQueueStateSchema,
  stateChangedAt: zx.dateTime,
})

export type JobQueueStatus = z.infer<typeof jobQueueStatusSchema>