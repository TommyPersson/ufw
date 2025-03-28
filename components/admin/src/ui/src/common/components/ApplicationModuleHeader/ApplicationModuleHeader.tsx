import Markdown from "react-markdown"
import { ApplicationModule } from "../../models"
import { PropertyGroup } from "../PropertyGroup"
import { PropertyText } from "../PropertyText"

export const ApplicationModuleHeader = (props: { applicationModule: ApplicationModule }) => {
  const { applicationModule } = props

  return (
    <PropertyGroup horizontal>
      <PropertyText
        title={"Application Module"}
        subtitle={applicationModule.name}
      />
      <PropertyText
        title={"Description"}
        subtitle={<Markdown>{applicationModule.description}</Markdown>}
        noSubtitleStyling
      />
      <PropertyText
        title={"ID"}
        subtitle={<code>{applicationModule.id}</code>}
      />
    </PropertyGroup>

  )
}