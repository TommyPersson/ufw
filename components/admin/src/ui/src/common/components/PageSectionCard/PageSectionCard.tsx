import { Card, CardContent } from "@mui/material"
import { PageSectionHeader } from "../PageSectionHeader"

export type PageSectionCardProps = {
  heading?: any
  children: any
}

export const PageSectionCard = (props: PageSectionCardProps) => {
  const { heading, children } = props

  return (
    <Card>
      <CardContent>
        {heading && <PageSectionHeader>{heading}</PageSectionHeader>}
        {children}
      </CardContent>
    </Card>
  )
}