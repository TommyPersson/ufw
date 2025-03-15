import { Paper } from "@mui/material"
import { useMemo } from "react"
import { Page, PageBreadcrumb } from "../../../../common/components"

export const CoreIndexPage = () => {

  const breadcrumbs = useMemo<PageBreadcrumb[]>(() => [
    { text: "Home", current: true },
  ], [])

  return (
    <Page
      breadcrumbs={breadcrumbs}
    >
      <Paper>Hello, World!</Paper>
    </Page>
  )
}