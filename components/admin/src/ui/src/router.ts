import { createBrowserRouter, RouterNavigateOptions, To } from "react-router"

export type Router = ReturnType<typeof createBrowserRouter>

export let routerHolder = {
  router: undefined as Router | undefined
}

export async function navigate(to: To | null, opts?: RouterNavigateOptions): Promise<void> {
  const router = routerHolder.router
  if (!router) {
    throw new Error("Router not setup correctly")
  }

  return await router.navigate(to, opts)
}