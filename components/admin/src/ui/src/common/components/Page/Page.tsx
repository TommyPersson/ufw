import { Box, Typography } from "@mui/material"
import classNames from "classnames"

import classes from "./Page.module.css"

export type PageProps = {
  heading?: any
  fill?: boolean
  children: any
}

export const Page = (props: PageProps) => {
  const {
    heading,
    fill = false,
    children
  } = props

  const className = classNames(
    classes.Page,
    fill ? classes.Filled : classes.NonFilled
  )

  return (
    <Box className={className}>
      {heading && <Typography variant={"h4"} component={"h2"} sx={{ mb: 2 }}>{heading}</Typography>}
      {children}
    </Box>
  )
}