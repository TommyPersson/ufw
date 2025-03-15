import { DateTime } from "luxon"
import { z, ZodObject, ZodRawShape } from "zod"

export const zx = {
  dateTime: z.string().transform(it => {
    const value = DateTime.fromISO(it)
    if (!value.isValid) {
      throw new Error(`Invalid DateTime: ${it}`)
    }

    return value as DateTime<true>
  }),
  paginatedList: <T extends ZodRawShape>(itemSchema: ZodObject<T>) => z.object({
    items: itemSchema.array(),
    hasMoreItems: z.boolean(),
  })
}