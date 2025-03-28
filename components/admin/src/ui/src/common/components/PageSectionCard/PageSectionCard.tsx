import { Card } from "@mui/material"
import { PageSectionHeader } from "../PageSectionHeader"

import classes from "./PageSectionCard.module.css"

export type PageSectionCardProps = {
  heading?: any
  children: any
}

export const PageSectionCard = (props: PageSectionCardProps) => {
  const { heading, children } = props

  return (
    <Card className={classes.PageSectionCard}>
      {heading && <PageSectionHeader className={classes.Heading}>{heading}</PageSectionHeader>}
      {children}
    </Card>
  )
}