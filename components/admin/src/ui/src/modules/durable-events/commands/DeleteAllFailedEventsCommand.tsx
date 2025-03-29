import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline"
import { makeApiRequest } from "../../../common/utils/api"
import { Command } from "../../../common/utils/commands"
import { queryClient } from "../../../common/utils/tsq"

export const DeleteAllFailedEventsCommand: Command<{ queueId: string }> = {
  mutationOptions: ({
    mutationKey: ['durable-events', 'DeleteAllFailedEvents'],
    mutationFn: async ({ queueId }) => {
      await makeApiRequest(`/admin/api/durable-events/queues/${queueId}/actions/delete-all-failed-events`, {
        method: "POST"
      })
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["durable-events", "queues"] })
    }
  }),
  label: "Delete all failed events",
  color: "error",
  icon: <DeleteOutlineIcon />,
  errorTitle: "Unable to delete events",
  confirmText: ({ queueId }) =>
    <>Are you sure you want to <strong>delete</strong> all failed events in the <code>{queueId}</code> queue?</>,
  confirmColor: "error",
}
