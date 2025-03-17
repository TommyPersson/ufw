import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline"
import { makeApiRequest } from "../../../common/utils/api"
import { Command } from "../../../common/utils/commands"
import { queryClient } from "../../../common/utils/tsq"
import { routerHolder } from "../../../router"

export const DeleteJobCommand: Command<{ queueId: string, jobId: string }> = {
  mutationOptions: ({
    mutationKey: ['durable-jobs', 'DeleteJob'],
    mutationFn: async ({ queueId, jobId }) => {
      await makeApiRequest(`/admin/api/durable-jobs/queues/${queueId}/jobs/${jobId}/actions/delete`, {
        method: "POST"
      })
    },
    onSuccess: async (_, { queueId }) => {
      await routerHolder.router?.navigate(`/durable-jobs/queues/${queueId}/details`)
      queryClient.invalidateQueries({ queryKey: ["durable-jobs", "queues"] }).then()
    }
  }),
  label: "Delete job",
  color: "error",
  icon: <DeleteOutlineIcon />,
  errorTitle: "Unable to delete job",
  confirmText: <>Are you sure you want to <strong>delete</strong> this job?</>,
  confirmColor: "error",
}
