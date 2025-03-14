import { Alert, AlertTitle, Box } from "@mui/material"
import { ApiError } from "../../utils/api"

export type ErrorAlertProps = {
  readonly error: Error | null
  readonly title?: string | null
  readonly onClose: () => void
}

export const ErrorAlert = (props: ErrorAlertProps) => {
  const { error, onClose } = props

  if (!error) {
    return null
  }

  const title = props.title || "An error occurred"

  let content: any = error.message
  if (error instanceof ApiError) {
    content = (
      <>
        <Box sx={{ mb: 1 }}>{error.data.errorMessage}</Box>
        <div><code>{error.statusCode}: {error.data.errorCode}</code></div>
      </>
    )
  }

  return (
    <Alert severity={"error"} onClose={onClose}>
      <AlertTitle>{title}</AlertTitle>
      {content}
    </Alert>
  )
}