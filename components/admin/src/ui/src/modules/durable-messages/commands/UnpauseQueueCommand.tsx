import PlayArrowIcon from "@mui/icons-material/PlayArrow"
import { makeApiRequest } from "../../../common/utils/api"
import { Command } from "../../../common/utils/commands"
import { queryClient } from "../../../common/utils/tsq"

export const UnpauseMessageQueueCommand: Command<{ queueId: string }> = {
  mutationOptions: ({
    mutationKey: ['durable-messages', 'UnpauseMessageQueue'],
    mutationFn: async ({ queueId }) => {
      await makeApiRequest(`/admin/api/durable-messages/queues/${queueId}/actions/unpause`, {
        method: "POST"
      })
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["durable-messages", "queues"] })
    }
  }),
  label: "Unpause",
  color: "primary",
  icon: <PlayArrowIcon />,
  errorTitle: "Unable to unpause queue",
}
