import { Box, Skeleton, Typography } from "@mui/material"
import { Theme } from "@mui/material/styles"
import { SxProps } from "@mui/system"

import classes from "./PropertyText.module.css"

export const PropertyText = (props: {
  title: string,
  subtitle: any,
  noSubtitleStyling?: boolean
  isLoading?: boolean
  sx?: SxProps<Theme>
}) => {

  let subtitle = props.noSubtitleStyling
    ? props.subtitle
    : <Typography variant={"subtitle2"}>{props.subtitle}</Typography>

  if (props.isLoading) {
    subtitle = <Skeleton />
  }

  return (
    <Box sx={props.sx} className={classes.PropertyText}>
      <Typography variant={"caption"}>{props.title}</Typography>
      {subtitle}
    </Box>
  )
}