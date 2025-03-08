export const ApplicationMetadataQuery = {
  queryKey: ["core", "application-metadata"],
  queryFn: async () => {
    return await (await fetch("/admin/api/core/application-metadata")).json()
  }
}