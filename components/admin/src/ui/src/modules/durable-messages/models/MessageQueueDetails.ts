import { z } from "zod"
import { applicationModuleSchema } from "../../../common/models"
import { messageQueueStatusSchema } from "./MessageQueueStatus"


export const messageQueueDetailsSchema = z.object({
  queueId: z.string(),
  numScheduled: z.number(),
  numPending: z.number(),
  numInProgress: z.number(),
  numFailed: z.number(),
  status: messageQueueStatusSchema,
  messageTypes: z.object({
    typeName: z.string(),
    className: z.string(),
    description: z.string().nullable(),
  }).array(),
  applicationModule: applicationModuleSchema,
})

export type MessageQueueDetails = z.infer<typeof messageQueueDetailsSchema>