import { z } from "zod"
import { applicationModuleSchema } from "../../../common/models"
import { eventQueueStatusSchema } from "./EventQueueStatus"

export const eventQueueListItemSchema = z.object({
  queueId: z.string(),
  numScheduled: z.number(),
  numPending: z.number(),
  numInProgress: z.number(),
  numFailed: z.number(),
  status: eventQueueStatusSchema,
  applicationModule: applicationModuleSchema,
})

export type EventQueueListItem = z.infer<typeof eventQueueListItemSchema>