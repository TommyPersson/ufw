import MoreVertOutlinedIcon from "@mui/icons-material/MoreVertOutlined"
import { IconButton, IconButtonProps, Menu, SvgIconProps } from "@mui/material"
import { useCallback, useState } from "react"

export const MoreOptionsMenuButton = (props: {
  buttonProps?: Partial<IconButtonProps>
  iconProps?: Partial<SvgIconProps>
  children: any
}) => {
  const { buttonProps, iconProps, children } = props

  const [anchor, setAnchor] = useState<HTMLElement | null>(null)
  const [isOpen, setIsOpen] = useState(false)

  const handleClick = useCallback(() => {
    setIsOpen(true)
  }, [setIsOpen])

  const handleClose = useCallback(() => {
    setIsOpen(false)
  }, [setIsOpen])

  return (
    <>
      <IconButton {...buttonProps} ref={setAnchor} onClick={handleClick}>
        <MoreVertOutlinedIcon {...iconProps} />
      </IconButton>
      <Menu
        open={isOpen}
        onClose={handleClose}
        onClick={handleClose}
        anchorEl={anchor}
      >
        {children}
      </Menu>
    </>
  )
}