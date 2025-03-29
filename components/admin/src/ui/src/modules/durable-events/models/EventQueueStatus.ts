import { z } from "zod"
import { zx } from "../../../common/utils/zod"
import { eventQueueStateSchema } from "./EventQueueState"

export const eventQueueStatusSchema = z.object({
  state: eventQueueStateSchema,
  stateChangedAt: zx.dateTime,
})

export type EventQueueStatus = z.infer<typeof eventQueueStatusSchema>