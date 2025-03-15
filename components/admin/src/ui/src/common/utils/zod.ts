import { DateTime } from "luxon"
import { z } from "zod"

export const zx = {
  dateTime: z.string().transform(it => {
    const value = DateTime.fromISO(it)
    if (!value.isValid) {
      throw new Error(`Invalid DateTime: ${it}`)
    }

    return value as DateTime<true>
  })
}