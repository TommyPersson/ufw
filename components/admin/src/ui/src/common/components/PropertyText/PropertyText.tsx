import { Box, Typography } from "@mui/material"
import { Theme } from "@mui/material/styles"
import { SxProps } from "@mui/system"

export const PropertyText = (props: {
  title: string,
  subtitle: any,
  noSubtitleStyling?: boolean
  sx?: SxProps<Theme>
}) => {
  const subtitle = props.noSubtitleStyling
    ? props.subtitle
    : <Typography variant={"subtitle2"}>{props.subtitle}</Typography>

  return (
    <Box sx={props.sx}>
      <Typography variant={"caption"}>{props.title}</Typography>
      {subtitle}
    </Box>
  )
}