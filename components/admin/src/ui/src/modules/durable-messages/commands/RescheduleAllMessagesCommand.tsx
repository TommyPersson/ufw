import PlaylistPlayIcon from "@mui/icons-material/PlaylistPlay"
import { makeApiRequest } from "../../../common/utils/api"
import { Command } from "../../../common/utils/commands"
import { queryClient } from "../../../common/utils/tsq"

export const RescheduleAllFailedMessagesCommand: Command<{ queueId: string }> = {
  mutationOptions: ({
    mutationKey: ["durable-messages", "RescheduleAllFailedMessages"],
    mutationFn: async ({ queueId }) => {
      await makeApiRequest(`/admin/api/durable-messages/queues/${queueId}/actions/reschedule-all-failed-messages`, {
        method: "POST"
      })
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["durable-messages", "queues"] })
    }
  }),
  label: "Reschedule all failed messages",
  icon: <PlaylistPlayIcon />,
  errorTitle: "Unable to reschedule messages",
  confirmText: ({ queueId }) =>
    <>Are you sure you want to <strong>reschedule</strong> all messages in the <code>{queueId}</code> queue?</>,
}
