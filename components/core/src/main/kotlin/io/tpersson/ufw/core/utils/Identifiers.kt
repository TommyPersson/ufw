
public val IdentifierRegex: Regex = Regex("^[a-zA-z0-9\\-_:.]+$")

// TODO validation libraries?
public fun String.requireMatch(regex: Regex, lazyMessage: () -> String = { "'${this}' does not match '${regex}'" }) {
    require(this.matches(regex), lazyMessage)
}

public fun String.requireValidIdentifier() {
    this.requireMatch(IdentifierRegex) {
        "'${this}' is not a valid identifier! (Regex = '${IdentifierRegex}')"
    }
}