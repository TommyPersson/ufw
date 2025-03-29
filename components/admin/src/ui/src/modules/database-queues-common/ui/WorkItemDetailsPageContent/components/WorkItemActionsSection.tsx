import { Box, ButtonProps } from "@mui/material"
import { CommandButton } from "../../../../../common/components"
import { WorkItemDetails } from "../../../models"
import { DatabaseQueueAdapterSettings } from "../../../DatabaseQueueAdapterSettings"


export const WorkItemActionsSection = (props: {
  details: WorkItemDetails | null | undefined
  adapterSettings: DatabaseQueueAdapterSettings<any, any>
}) => {
  const { details, adapterSettings } = props

  if (!details) {
    return null
  }

  const baseActionButtonProps: ButtonProps = {
    variant: "contained"
  }

  const { queueId, itemId, state } = details

  const canReschedule = state === "FAILED"
  const canDelete = state === "FAILED"
  const canCancel = state === "SCHEDULED" || state === "PENDING" || state === "IN_PROGRESS"

  if (!canReschedule && !canDelete && !canCancel) {
    return null
  }

  const commandArgs = { queueId, itemId }

  return (
    <Box display={"flex"} flexDirection={"row"} gap={1}>
      {canReschedule && (
        <CommandButton
          {...baseActionButtonProps}
          command={adapterSettings.commands.rescheduleItemNowCommand}
          args={adapterSettings.commands.rescheduleItemNowArgsTransform(commandArgs)}
        />
      )}
      {canDelete && (
        <CommandButton
          {...baseActionButtonProps}
          command={adapterSettings.commands.deleteItemCommand}
          args={adapterSettings.commands.deleteItemArgsTransform(commandArgs)}
        />
      )}
      {canCancel && (
        <CommandButton
          {...baseActionButtonProps}
          command={adapterSettings.commands.cancelItemCommand}
          args={adapterSettings.commands.cancelItemArgsTransform(commandArgs)}
        />
      )}
    </Box>
  )
}
