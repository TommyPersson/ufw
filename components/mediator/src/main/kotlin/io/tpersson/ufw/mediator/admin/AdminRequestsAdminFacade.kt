package io.tpersson.ufw.mediator.admin

import io.tpersson.ufw.mediator.admin.contracts.AdminRequestDTO

public interface AdminRequestsAdminFacade {
    public fun getRequests(requestType: RequestType): List<AdminRequestDTO>
}