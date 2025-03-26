import { Box, BoxProps } from "@mui/material"

export type PropertyGroupProps = {
  horizontal?: boolean
  boxProps?: Partial<BoxProps>
  children: any
}

export const PropertyGroup = (props: PropertyGroupProps) => {
  const { horizontal, children } = props

  const boxProps: Partial<BoxProps> = {
    display: "flex",
    flexDirection: horizontal ? "row" : "column",
    gap: horizontal ? 4 : 1,
    ...props.boxProps,
  }

  return (
    <Box {...boxProps}>
      {children}
    </Box>
  )
}