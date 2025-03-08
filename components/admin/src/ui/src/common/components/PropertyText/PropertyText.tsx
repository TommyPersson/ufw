import { Box, Typography } from "@mui/material"
import { Theme } from "@mui/material/styles"
import { SxProps } from "@mui/system"

export const PropertyText = (props: {
  title: string,
  subtitle: string,
  sx?: SxProps<Theme>
}) => {
  return (
    <Box sx={props.sx}>
      <Typography variant={"caption"}>{props.title}</Typography>
      <Typography variant={"subtitle2"}>{props.subtitle}</Typography>
    </Box>
  )
}