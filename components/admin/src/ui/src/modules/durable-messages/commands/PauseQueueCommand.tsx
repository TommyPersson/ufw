import PauseIcon from "@mui/icons-material/Pause"
import { makeApiRequest } from "../../../common/utils/api"
import { Command } from "../../../common/utils/commands"
import { queryClient } from "../../../common/utils/tsq"

export const PauseMessageQueueCommand: Command<{ queueId: string }> = {
  mutationOptions: ({
    mutationKey: ['durable-messages', 'PauseMessageQueue'],
    mutationFn: async ({ queueId }) => {
      await makeApiRequest(`/admin/api/durable-messages/queues/${queueId}/actions/pause`, {
        method: "POST"
      })
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["durable-messages", "queues"] })
    }
  }),
  label: "Pause",
  icon: <PauseIcon />,
  color: "error",
  errorTitle: "Unable to pause queue",
  confirmText: ({ queueId }) =>
    <>Are you sure you want to <strong>pause</strong> the <code>{queueId}</code> queue?</>,
}
