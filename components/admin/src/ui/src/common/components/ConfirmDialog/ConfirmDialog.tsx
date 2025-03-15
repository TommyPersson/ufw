import { AlertColor, Button, Dialog, DialogActions, DialogContent, DialogTitle } from "@mui/material"
import { useCallback } from "react"

export type ConfirmDialogProps = {
  isOpen: boolean
  title?: string
  content?: any
  color?: AlertColor
  onClose?: () => void
  onConfirm?: () => void
  onCancel?: () => void
}
export const ConfirmDialog = (props: ConfirmDialogProps) => {
  const { isOpen, onClose, onConfirm, onCancel } = props

  const handleCancelClick = useCallback(() => {
    onClose?.()
    onCancel?.()
  }, [onCancel, onClose])

  const handleConfirmClick = useCallback(() => {
    onClose?.()
    onConfirm?.()
  }, [onConfirm, onClose])

  const title = props.title ?? "Confirmation required"

  const content = props.content ?? "Please confirm"

  return (
    <Dialog open={isOpen} fullWidth onClose={onClose} closeAfterTransition={true}>
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>{content}</DialogContent>
      <DialogActions>
        <Button
          autoFocus
          children={"No"}
          onClick={handleCancelClick}
        />
        <Button
          variant={"contained"}
          children={"Yes"}
          color={props.color}
          onClick={handleConfirmClick}
        />
      </DialogActions>
    </Dialog>
  )
}


