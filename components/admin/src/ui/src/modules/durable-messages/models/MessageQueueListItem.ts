import { z } from "zod"
import { applicationModuleSchema } from "../../../common/models"
import { messageQueueStatusSchema } from "./MessageQueueStatus"

export const messageQueueListItemSchema = z.object({
  queueId: z.string(),
  numScheduled: z.number(),
  numPending: z.number(),
  numInProgress: z.number(),
  numFailed: z.number(),
  status: messageQueueStatusSchema,
  applicationModule: applicationModuleSchema,
})

export type MessageQueueListItem = z.infer<typeof messageQueueListItemSchema>