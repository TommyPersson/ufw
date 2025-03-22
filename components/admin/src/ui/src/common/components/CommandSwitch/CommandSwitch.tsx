import { Switch, Tooltip } from "@mui/material"
import { useMutation } from "@tanstack/react-query"
import { useCallback } from "react"
import { useConfirm } from "../../hooks"
import { Command } from "../../utils/commands"

export type CommandSwitchProps<TArgs> = {
  enableCommand: Command<TArgs>,
  disableCommand: Command<TArgs>,
  args: TArgs | null
  enabled: boolean
  disabled?: boolean
}
export const CommandSwitch = <TArgs, >(props: CommandSwitchProps<TArgs>) => {
  const {
    enableCommand,
    disableCommand,
    args,
    enabled,
  } = props

  const disabled = props.disabled || args === null

  const confirm = useConfirm()

  const command = enabled ? disableCommand : enableCommand

  const mutation = useMutation(command.mutationOptions)

  const handleClick = useCallback(async () => {
    if (args) {
      const { confirmed } = await confirm({
        content: command.confirmText,
        color: command.confirmColor,
      })

      if (confirmed) {
        mutation.mutate(args)
      }
    }
  }, [mutation, args])

  return (
    <Tooltip title={command.label}>
      <Switch
        checked={enabled}
        disabled={disabled}
        onClick={handleClick}
      />
    </Tooltip>
  )
}