import "./App.css"
import { useQuery } from "@tanstack/react-query"
import { ApplicationMetadataQuery } from "./queries/ApplicationMetadataQuery.tsx"
import { JobQueueAdminModuleView } from "./modules/job-queue/JobQueueAdminModuleView"

function App() {
  const applicationMetadataQuery = useQuery(ApplicationMetadataQuery)

  return (
    <>
      <p>Application metadata:</p>
      asd
      <p>
        {JSON.stringify(applicationMetadataQuery.data)}
      </p>
      <JobQueueAdminModuleView />
    </>
  )
}

export default App
