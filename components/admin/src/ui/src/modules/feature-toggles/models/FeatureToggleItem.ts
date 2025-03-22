import { z } from "zod"
import { zx } from "../../../common/utils/zod"

export const featureToggleItemSchema = z.object({
  id: z.string(),
  isEnabled: z.boolean(),
  stateChangedAt: zx.dateTime,
})

export type FeatureToggleItem = z.infer<typeof featureToggleItemSchema>