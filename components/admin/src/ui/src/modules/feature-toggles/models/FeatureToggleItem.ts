import { z } from "zod"
import { zx } from "../../../common/utils/zod"

export const featureToggleItemSchema = z.object({
  id: z.string(),
  title: z.string(),
  description: z.string(),
  stateChangedAt: zx.dateTime,
  createdAt: zx.dateTime,
  isEnabled: z.boolean(),
})

export type FeatureToggleItem = z.infer<typeof featureToggleItemSchema>