import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline"
import { makeApiRequest } from "../../../common/utils/api"
import { Command } from "../../../common/utils/commands"
import { queryClient } from "../../../common/utils/tsq"

export const DeleteAllFailedJobsCommand: Command<{ queueId: string }> = {
  mutationOptions: ({
    mutationFn: async ({ queueId }) => {
      await makeApiRequest(`/admin/api/durable-jobs/queues/${queueId}/actions/delete-all-failed-jobs`, {
        method: "POST"
      })
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["durable-jobs", "queues"] })
    }
  }),
  label: "Delete all failed jobs",
  color: "error",
  icon: <DeleteOutlineIcon />,
  errorTitle: "Unable to delete jobs",
  confirmText: <>Are you sure you want to <strong>delete</strong> all failed jobs?</>,
  confirmColor: "error",
}
