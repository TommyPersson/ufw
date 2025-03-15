import { z } from "zod"

export async function makeApiRequest<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, init)

  if (!response.ok) {
    try {
      const json = await response.json()
      const apiErrorData = apiErrorDataSchema.parse(json)
      throw new ApiError(apiErrorData, response.status)
    } catch (e) {
      if (e instanceof ApiError) {
        throw e
      }
    }

    throw Error(response.statusText)
  }

  if (response.status === 204) {
    return {} as T
  }

  return response.json()
}

const apiErrorDataSchema = z.object({
  errorCode: z.string(),
  errorMessage: z.string(),
})

export type ApiErrorData = z.infer<typeof apiErrorDataSchema>

export class ApiError extends Error {
  constructor(
    readonly data: ApiErrorData,
    readonly statusCode: number,
  ) {
    super(data.errorMessage)
  }
}

export type PaginatedList<TItem> = {
  items: TItem[]
  hasMoreItems: boolean
}