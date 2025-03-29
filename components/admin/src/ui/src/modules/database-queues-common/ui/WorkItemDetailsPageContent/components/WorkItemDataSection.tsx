import { CardContent } from "@mui/material"
import { JsonBlock, PageSectionCard, PageSectionHeader } from "../../../../../common/components"
import { WorkItemDetails } from "../../../models"

export const WorkItemDataSection = (props: {
  isLoading: boolean,
  details: WorkItemDetails | null | undefined
}) => {
  const { isLoading, details } = props

  return (
    <>
      <PageSectionHeader>Data</PageSectionHeader>
      <PageSectionCard heading={"Data JSON"}>
        <CardContent>
          <JsonBlock isLoading={isLoading} json={details?.dataJson ?? null} />
        </CardContent>
      </PageSectionCard>
      <PageSectionCard heading={"Metadata JSON"}>
        <CardContent>
          <JsonBlock isLoading={isLoading} json={details?.metadataJson ?? null} />
        </CardContent>
      </PageSectionCard>
    </>
  )
}