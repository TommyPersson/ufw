import { Button, ButtonProps, Popover } from "@mui/material"
import { useMutation } from "@tanstack/react-query"
import { useRef } from "react"
import { useConfirm } from "../../hooks"
import { Command, executeCommand } from "../../utils/commands"
import { ErrorAlert } from "../ErrorAlert"


export type CommandButton2Props<TArgs> = {
  command: Command<TArgs>,
  args: TArgs | null,
  onSuccess?: () => void
} & ButtonProps

export const CommandButton = <TArgs, >(props: CommandButton2Props<TArgs>) => {
  const { command, args, onSuccess, ...buttonProps } = props

  const confirm = useConfirm()

  const mutation = useMutation(command.mutationOptions)

  const buttonRef = useRef<HTMLButtonElement | null>(null)

  const handleClick = async () => {
    await executeCommand({ command, mutation, args, confirm, onSuccess })
  }

  return (
    <>
      <Button
        ref={buttonRef}
        {...buttonProps}
        color={command.color}
        children={command.label}
        startIcon={command.icon}
        onClick={handleClick}
        loading={mutation.isPending}
        disabled={args === null}
      />
      <Popover
        open={!!mutation.error}
        anchorEl={buttonRef.current}
        anchorOrigin={{ vertical: "bottom", horizontal: "left" }}
        onClose={mutation.reset}
      >
        <ErrorAlert title={command.errorTitle} error={mutation.error} onClose={mutation.reset} />
      </Popover>
    </>
  )
}
