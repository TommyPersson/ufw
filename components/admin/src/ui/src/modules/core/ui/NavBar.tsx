import HomeIcon from "@mui/icons-material/Home"
import { AppBar, IconButton, Toolbar, Typography } from "@mui/material"
import { PropertyText } from "../../../common/components"
import { ApplicationMetadata } from "../models/ApplicationMetadata"

export const NavBar = (props: {
  applicationMetadata: ApplicationMetadata
}) => {
  return (
    <AppBar
      position={"fixed"}
      sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}
    >
      <Toolbar>
        <IconButton
          size={"large"}
          edge={"start"}
          color={"inherit"}
          children={<HomeIcon />}
          sx={{ mr: 2 }}
        />
        <Typography
          variant={"h6"}
          component={"div"}
          children={"UFW Admin"}
          sx={{ mr: 2 }}
        />
        <PropertyText
          title={"Application"}
          subtitle={props.applicationMetadata.name}
          sx={{ mr: 2 }}
        />
        <PropertyText
          title={"Version"}
          subtitle={props.applicationMetadata.version}
          sx={{ mr: 2 }}
        />
      </Toolbar>
    </AppBar>
  )
}