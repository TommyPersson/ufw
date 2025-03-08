import { Box } from "@mui/material"
import { useQuery } from "@tanstack/react-query"
import { defaultApplicationMetadata } from "../models/ApplicationMetadata"
import { ApplicationMetadataQuery } from "../queries/ApplicationMetadataQuery.tsx"
import { NavBar } from "./NavBar"
import { NavDrawer } from "./NavDrawer"
import { PageContainer } from "./PageContainer"

function Root() {
  const applicationMetadataQuery = useQuery(ApplicationMetadataQuery)
  const applicationMetadata = applicationMetadataQuery.data ?? defaultApplicationMetadata

  return (
    <Box flex={1} sx={{ display: "flex" }}>
      <NavBar applicationMetadata={applicationMetadata} />
      <NavDrawer applicationMetadata={applicationMetadata} />
      <PageContainer />
    </Box>
  )
}

export default Root
