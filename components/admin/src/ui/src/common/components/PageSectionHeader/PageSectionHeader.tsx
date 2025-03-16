import { Typography } from "@mui/material"

export const PageSectionHeader = (props: { children: any }) => {
  return (
    <Typography variant={"h5"} component={"h3"}>{props.children}</Typography>
  )
}