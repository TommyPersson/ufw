import { Button, ButtonProps, Popover } from "@mui/material"
import { UseMutationResult } from "@tanstack/react-query"
import { useRef } from "react"
import { useConfirm } from "../../hooks"
import { ErrorAlert } from "../ErrorAlert"


export type CommandButtonProps<TArgs> = {
  command: UseMutationResult<any, any, TArgs>,
  args: TArgs
  errorTitle?: string | null
  confirmText?: string | null
} & ButtonProps

export const CommandButton = <TArgs, >(props: CommandButtonProps<TArgs>) => {
  const { command, args, confirmText, errorTitle, ...buttonProps } = props

  const buttonRef = useRef<HTMLButtonElement | null>(null)

  return (
    <>
      <Button
        ref={buttonRef}
        {...buttonProps}
        {...bindCommandButton(command, args, confirmText ?? null)}
      />
      <Popover
        open={command.error}
        anchorEl={buttonRef.current}
        anchorOrigin={{ vertical: "bottom", horizontal: "left" }}
        onClose={command.reset}
      >
        <ErrorAlert title={errorTitle} error={command.error} onClose={command.reset} />
      </Popover>
    </>
  )
}

export function bindCommandButton<TArgs>(
  mutation: UseMutationResult<any, any, TArgs>,
  args: TArgs | null,
  confirmText: string | null,
): ButtonProps {
  const confirm = useConfirm()

  const handleClick = async () => {
    if (args) {
      if (confirmText) {
        const { confirmed } = await confirm({
          content: confirmText,
          color: "error"
        })

        if (!confirmed) {
          return
        }
      }

      mutation.mutate(args)
    }
  }

  return {
    onClick: handleClick,
    loading: mutation.isPending,
    disabled: args === null
  }
}