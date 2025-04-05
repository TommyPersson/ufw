import { ListItemIcon, ListItemText, MenuItem, MenuItemProps, Popover } from "@mui/material"
import { useMutation } from "@tanstack/react-query"
import { useRef } from "react"
import { useConfirm } from "../../hooks"
import { Command, executeCommand } from "../../utils/commands"
import { ErrorAlert } from "../ErrorAlert"


export type CommandMenuItemProps<TArgs> = {
  command: Command<TArgs>,
  args: TArgs | null,
  onSuccess?: () => void
} & MenuItemProps

export const CommandMenuItem = <TArgs, >(props: CommandMenuItemProps<TArgs>) => {
  const { command, args, onSuccess, ...menuItemProps } = props

  const confirm = useConfirm()

  const mutation = useMutation(command.mutationOptions)

  const menuItemRef = useRef<HTMLLIElement | null>(null)

  const handleClick = async () => {
    await executeCommand({ command, mutation, args, confirm, onSuccess })
  }

  return (
    <>
      <MenuItem
        ref={menuItemRef}
        {...menuItemProps}
        onClick={handleClick}
        disabled={args === null || mutation.isPending}
      >
        {command.icon && (
          <ListItemIcon>
            {command.icon}
          </ListItemIcon>
        )}
        <ListItemText>
          {command.label}{command.confirmText ? " ..." : ""}
        </ListItemText>
      </MenuItem>
      <Popover
        open={!!mutation.error}
        anchorEl={menuItemRef.current}
        anchorOrigin={{ vertical: "bottom", horizontal: "left" }}
        onClose={mutation.reset}
      >
        <ErrorAlert title={command.errorTitle} error={mutation.error} onClose={mutation.reset} />
      </Popover>
    </>
  )
}
