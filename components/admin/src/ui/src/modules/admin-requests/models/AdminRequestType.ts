import { z } from "zod"

export const adminRequestTypeSchema = z.enum(["COMMAND", "QUERY"])

export type AdminRequestType = "COMMAND" | "QUERY"