import { ButtonProps } from "@mui/material"
import { createRoot } from "react-dom/client"
import { ConfirmDialog, confirmProviderRootId } from "../components"

export type ConfirmOptions = {
  title?: string
  content?: any
  color?: ButtonProps["color"]
}

export type ConfirmResult = {
  confirmed: boolean
}

export type UseConfirmResult = (options: ConfirmOptions) => Promise<ConfirmResult>

export function useConfirm(): UseConfirmResult {

  return (options: ConfirmOptions) => new Promise((resolve, reject) => {
    const rootElement = document.getElementById(confirmProviderRootId)
    if (!rootElement) {
      reject("Unable to find ConfirmProvider portal element")
      return
    }

    const handleConfirm = () => {
      resolve({ confirmed: true })
    }

    const handleCancel = () => {
      resolve({ confirmed: false })
    }

    const handleClose = () => {
      root.unmount()
    }

    const root = createRoot(rootElement)
    root.render(
      <ConfirmDialog
        {...options}
        isOpen={true}
        onConfirm={handleConfirm}
        onCancel={handleCancel}
        onClose={handleClose}
      />)
  })
}