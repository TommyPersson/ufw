
export type ApplicationMetadata = {
  name: string,
  version: string,
  availableModuleIds: string[]
}

export const defaultApplicationMetadata: ApplicationMetadata = {
  name: "Unknown",
  version: "0",
  availableModuleIds: []
}