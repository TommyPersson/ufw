import { CardContent } from "@mui/material"
import Markdown from "react-markdown"
import { PageSectionCard, PageSectionHeader, PropertyGroup, PropertyText } from "../../../../../common/components"
import { WorkItemType } from "../../../models/WorkItemType"
import { DatabaseQueueAdapterSettings } from "../../../DatabaseQueueAdapterSettings"

export const WorkItemTypesSection = (props: {
  workItemTypes: WorkItemType[]
  adapterSettings: DatabaseQueueAdapterSettings
}) => {
  const { workItemTypes, adapterSettings } = props

  return (
    <>
      <PageSectionHeader>{adapterSettings.texts.queueTypeSingular} Types</PageSectionHeader>
      {workItemTypes.map(it => (
        <WorkItemTypeDetailsCard key={it.typeName} type={it} />
      ))}
    </>
  )
}

const WorkItemTypeDetailsCard = (props: { type: WorkItemType }) => {
  return (
    <PageSectionCard>
      <CardContent>
        <PropertyGroup>
          <PropertyGroup horizontal>
            <PropertyText
              title={"Type"}
              subtitle={<code>{props.type.typeName}</code>}
            />
            <PropertyText
              title={"Class Name"}
              subtitle={<code>{props.type.className}</code>}
            />
          </PropertyGroup>
          <PropertyText
            title={"Description"}
            subtitle={<Markdown>{props.type.description ?? "*No description*"}</Markdown>}
            noSubtitleStyling
          />
        </PropertyGroup>
      </CardContent>
    </PageSectionCard>
  )
}