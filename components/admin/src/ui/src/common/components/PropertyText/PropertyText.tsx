import { Box, Skeleton, Typography } from "@mui/material"
import { Theme } from "@mui/material/styles"
import { SxProps } from "@mui/system"

import classes from "./PropertyText.module.css"

export const PropertyText = (props: {
  title: string,
  subtitle: any | null | undefined,
  fallback?: any
  noSubtitleStyling?: boolean
  isLoading?: boolean
  sx?: SxProps<Theme>
}) => {

  const subtitleText = props.subtitle !== null && props.subtitle !== undefined
    ? props.subtitle
    : (props.fallback ?? <em>N/A</em> );

  let subtitle = props.noSubtitleStyling
    ? <Typography variant={"subtitle2"} style={{ fontWeight: 'normal' }} component={"span"}>{subtitleText}</Typography>
    : <Typography variant={"subtitle2"}>{subtitleText}</Typography>

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