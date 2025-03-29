import { CardContent, Divider } from "@mui/material"
import Markdown from "react-markdown"
import { DateTimeText, PageSectionCard, PropertyGroup, PropertyText } from "../../../../../common/components"
import { WorkItemDetails } from "../../../models"

export const WorkItemDetailsSection = (props: {
  isLoading: boolean,
  details: WorkItemDetails | null | undefined
}) => {
  const { isLoading, details } = props

  return (
    <PageSectionCard heading={"Details"}>
      <CardContent>
        <PropertyGroup>
          <PropertyText
            title={"ID"}
            isLoading={isLoading}
            subtitle={<code>{details?.itemId}</code>}
          />
          <PropertyGroup horizontal>
            <PropertyGroup boxProps={{ flex: 1 }}>
              <PropertyText
                title={"State"}
                isLoading={isLoading}
                subtitle={<code>{details?.state}</code>}
              />
              <PropertyText
                title={"# Failures"}
                isLoading={isLoading}
                subtitle={details?.numFailures}
              />
            </PropertyGroup>
            <PropertyGroup boxProps={{ flex: 1 }}>
              <PropertyText
                title={"Created At"}
                isLoading={isLoading}
                subtitle={<DateTimeText dateTime={details?.createdAt ?? null} />}
              />
              <PropertyText
                title={"First Scheduled For"}
                isLoading={isLoading}
                subtitle={<DateTimeText dateTime={details?.firstScheduledFor ?? null} />}
              />
              <PropertyText
                title={"Next Scheduled For"}
                isLoading={isLoading}
                subtitle={<DateTimeText dateTime={details?.nextScheduledFor ?? null} fallback={<em>N/A</em>} />}
              />
              <PropertyText
                title={"State Changed At"}
                isLoading={isLoading}
                subtitle={<DateTimeText dateTime={details?.stateChangedAt ?? null} />}
              />
              <PropertyText
                title={"Expires At"}
                isLoading={isLoading}
                subtitle={<DateTimeText dateTime={details?.expiresAt ?? null} fallback={<em>N/A</em>} />}
              />
            </PropertyGroup>
            <PropertyGroup boxProps={{ flex: 1 }}>
              <PropertyText
                title={"Concurrency Key"}
                isLoading={isLoading}
                subtitle={details?.concurrencyKey ? <code>{details.concurrencyKey}</code> : <em>N/A</em>}
              />
              <PropertyText
                title={"Watchdog Owner"}
                isLoading={isLoading}
                subtitle={details?.watchdogOwner ? <code>{details.watchdogOwner}</code> : <em>N/A</em>}
              />
              <PropertyText
                title={"Watchdog Timestamp"}
                isLoading={isLoading}
                subtitle={<DateTimeText dateTime={details?.watchdogTimestamp ?? null} fallback={<em>N/A</em>} />}
              />
            </PropertyGroup>
          </PropertyGroup>
        </PropertyGroup>
      </CardContent>
      <Divider />
      <CardContent>
        <PropertyGroup>
          <PropertyGroup horizontal>
            <PropertyText
              title={"Type"}
              isLoading={isLoading}
              subtitle={<code>{details?.itemType}</code>}
            />
            <PropertyText
              title={"Class Name"}
              isLoading={isLoading}
              subtitle={<code>{details?.itemTypeClass}</code>}
            />
          </PropertyGroup>
          <PropertyText
            title={"Type Description"}
            isLoading={isLoading}
            noSubtitleStyling={!!details?.itemTypeClass}
            subtitle={details?.itemTypeClass ? <Markdown>{details?.itemTypeClass}</Markdown> :
              <em>N/A</em>}
          />
        </PropertyGroup>
      </CardContent>
    </PageSectionCard>
  )
}