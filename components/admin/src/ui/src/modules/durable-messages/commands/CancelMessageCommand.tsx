import PlaylistRemoveIcon from "@mui/icons-material/PlaylistRemoveOutlined"
import { makeApiRequest } from "../../../common/utils/api"
import { Command } from "../../../common/utils/commands"
import { queryClient } from "../../../common/utils/tsq"

export const CancelMessageCommand: Command<{ queueId: string, messageId: string }> = {
  mutationOptions: ({
    mutationKey: ["durable-messages", "CancelMessage"],
    mutationFn: async ({ queueId, messageId }) => {
      await makeApiRequest(`/admin/api/durable-messages/queues/${queueId}/messages/${messageId}/actions/cancel`, {
        method: "POST"
      })
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["durable-messages", "queues"] })
    }
  }),
  label: "Cancel message",
  color: "error",
  icon: <PlaylistRemoveIcon />,
  errorTitle: "Unable to cancel message",
  confirmText: <>
    Are you sure you want to <strong>cancel</strong> this message?<br />
    You will not be able to reschedule it.
  </>,
  confirmColor: "error",
}
