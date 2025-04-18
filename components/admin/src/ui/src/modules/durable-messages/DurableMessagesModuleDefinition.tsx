import InboxOutlinedIcon from "@mui/icons-material/InboxOutlined"
import { ModuleDefinition } from "../ModuleDefinition.tsx"
import { MessageDetailsPage } from "./ui/pages/MessageDetailsPage"
import { MessageListPage } from "./ui/pages/MessageListPage"
import { MessageQueueDetailsPage } from "./ui/pages/MessageQueueDetailsPage"
import { MessageQueueIndexPage } from "./ui/pages/MessageQueueIndexPage.tsx"

export const DurableMessagesModuleDefinition: ModuleDefinition = {
  moduleId: "durable-messages",
  navItems: [{
    title: "Durable Messages",
    route: "durable-messages",
    icon: <InboxOutlinedIcon />,
  }],
  routes: [
    {
      path: "durable-messages",
      children: [
        {
          index: true,
          Component: MessageQueueIndexPage,
        },
        {
          path: "queues/:queueId/details",
          Component: MessageQueueDetailsPage,
        },
        {
          path: "queues/:queueId/messages/:messageState",
          Component: MessageListPage,
        },
        {
          path: "queues/:queueId/messages/by-id/:messageId/details",
          Component: MessageDetailsPage
        }
      ]
    }
  ]
}
