package io.tpersson.ufw.examples.common.queries

import io.tpersson.ufw.mediator.*
import io.tpersson.ufw.mediator.annotations.AdminRequest

@AdminRequest(
    name = "Test Query #1",
    description =  "It does whatever a Test Query #1 does"
)
public data class TestAdminQuery1(
    val input: String
) : Query<String>

public class TestAdminQuery1Handler : QueryHandler<TestAdminQuery1, String> {
    override suspend fun handle(query: TestAdminQuery1, context: Context): String {
        return query.input.reversed()
    }
}