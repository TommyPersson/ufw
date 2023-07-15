package io.tpersson.ufw.mediator.middleware

/**
 * Lower (smaller integer) priorities are applied "closer to" to the actual request handler.
 */
public object StandardMiddlewarePriorities {
    public const val TimeLimited: Int = 400
    public const val Cacheable: Int = 300
    public const val Retryable: Int = 200
    public const val Transactional: Int = 100
}