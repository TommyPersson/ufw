import ExpandLessIcon from "@mui/icons-material/ExpandLess"
import ExpandMoreIcon from "@mui/icons-material/ExpandMore"
import { Alert, Card, IconButton, Skeleton } from "@mui/material"
import { useState } from "react"
import {
  CodeBlock,
  DateTimeText,
  PageSectionHeader,
  PropertyGroup,
  PropertyText
} from "../../../../../common/components"
import { WorkItemFailure } from "../../../common/models"

export const WorkItemFailuresSection = (props: {
  isLoading: boolean,
  failures: WorkItemFailure[]
}) => {
  const { isLoading, failures } = props

  return (
    <>
      <PageSectionHeader>Last 5 Failures</PageSectionHeader>
      {isLoading && <Skeleton />}
      {!isLoading && failures.length === 0 && <NoFailuresMessage />}
      {failures.map((it, i) => (
        <WorkItemFailureCard key={it.failureId} failure={it} isFirst={i === 0} />
      ))}
    </>
  )
}

const WorkItemFailureCard = (props: { failure: WorkItemFailure, isFirst: boolean }) => {
  const { failure, isFirst } = props

  const [isExpanded, setIsExpanded] = useState(isFirst)

  const toggleExpansionButton = (
    <IconButton
      onClick={() => setIsExpanded(s => !s)}
      children={isExpanded ? <ExpandLessIcon /> : <ExpandMoreIcon />}
    />
  )

  return (
    <Card key={failure.failureId}>
      <Alert severity={"error"} action={toggleExpansionButton}>
        <PropertyGroup>
          <PropertyGroup horizontal>
            <PropertyText
              title={"Error Type"}
              subtitle={<code>{failure.errorType}</code>}
            />
            <PropertyText
              title={"Timestamp"}
              subtitle={<DateTimeText dateTime={failure.timestamp} />}
            />
          </PropertyGroup>
          {isExpanded && (
            <PropertyGroup>
              <PropertyText
                title={"Error Message"}
                subtitle={<code>{failure.errorMessage}</code>}
              />
              <PropertyText
                title={"Error Stacktrace"}
                subtitle={<CodeBlock code={failure.errorStackTrace} style={{ height: 160 }} />}
              />
            </PropertyGroup>
          )}
        </PropertyGroup>
      </Alert>
    </Card>
  )
}

const NoFailuresMessage = () => {
  return (<Alert severity={"success"}>No failures has been recorded</Alert>)
}