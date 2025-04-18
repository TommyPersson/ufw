import { z } from "zod"
import { zx } from "../../../common/utils/zod"
import { messageQueueStateSchema } from "./MessageQueueState"

export const messageQueueStatusSchema = z.object({
  state: messageQueueStateSchema,
  stateChangedAt: zx.dateTime,
})

export type MessageQueueStatus = z.infer<typeof messageQueueStatusSchema>