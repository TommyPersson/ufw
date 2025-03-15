
export const confirmProviderRootId = "__confirm_provider_root"

export type ConfirmProviderProps = { children: any }

export const ConfirmProvider = (props: ConfirmProviderProps) => {
  return (
    <>
      {props.children}
      <div id={confirmProviderRootId}></div>
    </>
  )
}
