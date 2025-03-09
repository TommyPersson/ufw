import { Portal } from "@mui/material"

export type NavBarPortalProps = {
  children?: any
}

export const navBarPortalContainerId = "nav-bar-portal-container"

export const NavBarPortal = (props: NavBarPortalProps) => {
  const portalTarget = document.getElementById(navBarPortalContainerId)
  if (!portalTarget) {
    return null
  }

  return (
    <Portal container={portalTarget}>
      {props.children}
    </Portal>
  )
}