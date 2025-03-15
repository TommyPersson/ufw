
export const confirmProviderRootId = "__confirm_provider_portal"

export type ConfirmProviderProps = { children: any }

export const ConfirmProvider = (props: ConfirmProviderProps) => {
  return (
    <>
      <div id={confirmProviderRootId}></div>
      {props.children}
    </>
  )
}
