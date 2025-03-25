package io.tpersson.ufw.keyvaluestore.storageengine

public enum class EntryType(public val int: Int) {
    JSON(1),
    Bytes(2),
}

public sealed class EntryValue(public val type: EntryType) {
    public data class Json(val json: String) : EntryValue(EntryType.JSON)
    public data class Bytes(val bytes: ByteArray) : EntryValue(EntryType.Bytes)
}