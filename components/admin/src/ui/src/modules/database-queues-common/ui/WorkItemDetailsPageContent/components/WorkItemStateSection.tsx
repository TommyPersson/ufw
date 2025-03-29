import { Alert, AlertTitle, Box } from "@mui/material"
import { DateTimeText } from "../../../../../common/components"
import { WorkItemDetails, WorkItemFailure } from "../../../common/models"
import { DatabaseQueueAdapterSettings } from "../../../DatabaseQueueAdapterSettings"

export const WorkItemStateSection = (props: {
  isLoading: boolean
  details: WorkItemDetails | null | undefined
  lastFailure: WorkItemFailure | null | undefined
  adapterSettings: DatabaseQueueAdapterSettings
}) => {
  const { isLoading, details, lastFailure, adapterSettings } = props

  if (isLoading || !details) {
    return null
  }

  const queueType = adapterSettings.texts.queueTypeSingular.toLowerCase()

  switch (details.state) {
    case "SCHEDULED":
      return (
        <Alert severity={"info"}>
          <AlertTitle>{details.state}</AlertTitle>
          This {queueType} is scheduled to execute at{' '}
          <strong><DateTimeText dateTime={details.nextScheduledFor} /></strong>.
        </Alert>
      )
    case "PENDING":
      return (
        <Alert severity={"info"}>
          <AlertTitle>{details.state}</AlertTitle>
          This {queueType} is waiting to execute since{' '}
          <strong><DateTimeText dateTime={details.nextScheduledFor} /></strong>.
        </Alert>
      )
    case "IN_PROGRESS":
      return (
        <Alert severity={"info"}>
          <AlertTitle>{details.state}</AlertTitle>
          This {queueType} started at{' '}
          <strong><DateTimeText dateTime={details.stateChangedAt} /></strong>.
        </Alert>
      )
    case "CANCELLED":
      return (
        <Alert severity={"warning"}>
          <AlertTitle>{details.state}</AlertTitle>
          This {queueType} was cancelled at{' '}
          <strong><DateTimeText dateTime={details.stateChangedAt} /></strong>.
        </Alert>
      )
    case "SUCCESSFUL":
      return (
        <Alert severity={"success"}>
          <AlertTitle>{details.state}</AlertTitle>
          This {queueType} finished successfully at{' '}
          <strong><DateTimeText dateTime={details.stateChangedAt} /></strong>.
        </Alert>
      )
    case "FAILED":
      return (
        <Alert severity={"error"}>
          <AlertTitle>{details.state}</AlertTitle>
          <Box sx={{ mb: 1 }}>
            This {queueType} failed at <strong><DateTimeText dateTime={details.stateChangedAt} /></strong>.
          </Box>
          <Box>
            The most recent error was <code>{lastFailure?.errorType}</code>, see the failure list below for details.
          </Box>
        </Alert>
      )
    default:
      return null
  }
}