import PlaylistRemoveIcon from "@mui/icons-material/PlaylistRemoveOutlined"
import { makeApiRequest } from "../../../common/utils/api"
import { Command } from "../../../common/utils/commands"
import { queryClient } from "../../../common/utils/tsq"

export const CancelJobCommand: Command<{ queueId: string, jobId: string }> = {
  mutationOptions: ({
    mutationKey: ["durable-jobs", "CancelJob"],
    mutationFn: async ({ queueId, jobId }) => {
      await makeApiRequest(`/admin/api/durable-jobs/queues/${queueId}/jobs/${jobId}/actions/cancel`, {
        method: "POST"
      })
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["durable-jobs", "queues"] })
    }
  }),
  label: "Cancel job",
  color: "error",
  icon: <PlaylistRemoveIcon />,
  errorTitle: "Unable to cancel job",
  confirmText: <>
    Are you sure you want to <strong>cancel</strong> this job?<br />
    You will not be able to reschedule it.
  </>,
  confirmColor: "error",
}
