import { UseQueryOptions } from "@tanstack/react-query"
import { z } from "zod"
import { makeApiRequest } from "../../../common/utils/api"
import { AdminRequest, adminRequestSchema, AdminRequestType } from "../models"

export const AdminRequestsQuery: (requestType: AdminRequestType) => UseQueryOptions<AdminRequest[]> = (requestType) => ({
  queryKey: ["admin-requests", requestType],
  queryFn: async () => {
    let subPath = ""
    if (requestType == "COMMAND") {
      subPath = "commands"
    } else {
      subPath = "queries"
    }

    return responseSchema.parse(await makeApiRequest(
      `/admin/api/admin-requests/${subPath}`
    ))
  },
  retry: false,
})

const responseSchema = z.array(adminRequestSchema)