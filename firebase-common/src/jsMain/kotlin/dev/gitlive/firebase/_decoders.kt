/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase

import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlin.js.Json

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
actual fun FirebaseDecoder.structureDecoder(descriptor: SerialDescriptor): CompositeDecoder = when(descriptor.kind) {
    StructureKind.CLASS, StructureKind.OBJECT -> (value as Json).let { json ->
        FirebaseClassDecoder(js("Object").keys(value).length as Int, serializersModule, { json[it] != undefined }) {
                desc, index -> json[desc.getElementName(index)]
        }
    }
    StructureKind.LIST, is PolymorphicKind -> (value as Array<*>).let {
        FirebaseCompositeDecoder(it.size, serializersModule) { _, index -> it[index] }
    }
    StructureKind.MAP -> (js("Object").entries(value) as Array<Array<Any>>).let {
        FirebaseCompositeDecoder(it.size, serializersModule) { _, index -> it[index/2].run { if(index % 2 == 0) get(0) else get(1) } }
    }
    else -> TODO("Not implemented ${descriptor.kind}")
}
