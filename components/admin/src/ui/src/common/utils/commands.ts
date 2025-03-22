import { ButtonProps, IconProps } from "@mui/material"
import { UseMutationOptions, UseMutationResult } from "@tanstack/react-query"
import * as React from "react"
import { UseConfirmResult } from "../hooks"

export type Command<TArgs> = {
  mutationOptions: UseMutationOptions<any, Error, TArgs>,
  label: any
  color?: ButtonProps["color"]
  icon?: React.ReactElement<IconProps>
  errorTitle?: string
  confirmText?: React.ReactElement | ((args: TArgs) => React.ReactElement)
  confirmColor?: ButtonProps["color"]
}

export async function executeCommand<TArgs>(
  command: Command<TArgs>,
  mutation: UseMutationResult<any, Error, TArgs>,
  args: TArgs | null,
  confirm: UseConfirmResult
) {
  if (!args) {
    return
  }

  if (command.confirmText) {
    let content = command.confirmText
    if (typeof command.confirmText === "function") {
      content = command.confirmText(args)
    }

    const { confirmed } = await confirm({
      content: content,
      color: command.confirmColor,
    })

    if (!confirmed) {
      return
    }
  }

  mutation.mutate(args)
}