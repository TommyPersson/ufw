import { ButtonProps, IconProps } from "@mui/material"
import { UseMutationOptions } from "@tanstack/react-query"
import * as React from "react"

export type Command<TArgs> = {
  mutationOptions: UseMutationOptions<any, Error, TArgs>,
  label: any
  color?: ButtonProps["color"]
  icon?: React.ReactElement<IconProps>
  errorTitle?: string
  confirmText?: any
  confirmColor?: ButtonProps["color"]
}