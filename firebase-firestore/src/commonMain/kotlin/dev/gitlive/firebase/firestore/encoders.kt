package dev.gitlive.firebase.firestore

/** @return whether value is special and shouldn't be encoded/decoded. */
@PublishedApi
internal expect fun isSpecialValue(value: Any): Boolean

@PublishedApi
internal inline fun <reified T> encode(value: T, shouldEncodeElementDefault: Boolean) =
    if (value?.let(::isSpecialValue) == true) {
        value
    } else {
        dev.gitlive.firebase.encode(value, shouldEncodeElementDefault)
    }
