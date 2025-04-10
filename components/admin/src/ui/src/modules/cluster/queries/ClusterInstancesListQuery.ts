import { UseQueryOptions } from "@tanstack/react-query"
import { makeApiRequest, PaginatedList } from "../../../common/utils/api"
import { zx } from "../../../common/utils/zod"
import { ClusterInstance, clusterInstanceSchema } from "../models"


const ClusterInstancesListQueryKey = ['cluster', 'instances']

export const ClusterInstancesListQuery: UseQueryOptions<PaginatedList<ClusterInstance>> = ({
  queryKey: ClusterInstancesListQueryKey,
  queryFn: async () => {
    return zx.paginatedList(clusterInstanceSchema).parse(await makeApiRequest(`/admin/api/cluster/instances`))
  }
})