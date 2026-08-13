package dev.sunls24.biliapi.repositories

import dev.sunls24.biliapi.grpc.utils.generateChannel
import io.grpc.ManagedChannel
import org.koin.core.annotation.Single

@Single
class ChannelRepository {
    // grpc.biliapi.net
    var defaultChannel: ManagedChannel? = null
        private set

    fun initDefaultChannel(accessKey: String?, buvid: String) {
        defaultChannel?.shutdownNow()
        defaultChannel = accessKey
            ?.takeIf { it.isNotBlank() }
            ?.let { generateChannel(it, buvid) }
    }
}
