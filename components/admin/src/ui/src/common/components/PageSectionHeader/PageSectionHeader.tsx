import { Typography } from "@mui/material"

export type PageSectionHeaderProps = {
  className?: string
  children: any
}

export const PageSectionHeader = (props: PageSectionHeaderProps) => {
  return (
    <Typography
      variant={"h5"}
      component={"h3"}
      className={props.className}
      children={props.children}
    />
  )
}