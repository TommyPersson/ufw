import { UseMutationOptions } from "@tanstack/react-query"
import { makeApiRequest } from "../../../common/utils/api"
import { AdminRequestType } from "../models"

export const ExecuteRequestMutation: UseMutationOptions<any, any, {
  fqcn: string,
  body: any,
  requestType: AdminRequestType
}> = ({
  mutationFn: async ({ fqcn, body, requestType }) => {
    let subPath = ""
    if (requestType == "COMMAND") {
      subPath = "commands"
    } else {
      subPath = "queries"
    }

    // TODO response types

    const response = await makeApiRequest(`/admin/api/admin-requests/${subPath}/${fqcn}/actions/execute`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(body),
    })

    return response
  }
})