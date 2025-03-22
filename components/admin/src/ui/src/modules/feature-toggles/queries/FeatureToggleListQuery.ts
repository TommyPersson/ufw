import { UseQueryOptions } from "@tanstack/react-query"
import { makeApiRequest, PaginatedList } from "../../../common/utils/api"
import { zx } from "../../../common/utils/zod"
import { FeatureToggleItem, featureToggleItemSchema } from "../models"

export const FeatureToggleListQueryKeyPrefix = ["feature-toggles", "feature-toggles"]

export const FeatureToggleListQuery: (page: number) => UseQueryOptions<PaginatedList<FeatureToggleItem>> =
  (page) => ({
    queryKey: [...FeatureToggleListQueryKeyPrefix, page],
    queryFn: async () => {
      const limit = 100
      const offset = (page - 1) * 100
      return responseSchema.parse(await makeApiRequest(`/admin/api/feature-toggles/feature-toggles?limit=${limit}&offset=${offset}`))
    },
  })

const responseSchema = zx.paginatedList(
  featureToggleItemSchema
)
