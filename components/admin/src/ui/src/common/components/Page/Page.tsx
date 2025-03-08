import { Box } from "@mui/material"
import classNames from "classnames"

import classes from "./Page.module.css"

export type PageProps = {
  fill?: boolean
  children: any
}

export const Page = (props: PageProps) => {
  const {
    fill = false,
  } = props

  const className = classNames(
    classes.Page,
    fill ? classes.Filled : classes.NonFilled
  )

  return (
    <Box className={className}>
      {props.children}
    </Box>
  )
}