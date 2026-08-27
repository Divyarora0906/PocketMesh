package com.pocketmesh.app
import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkUtils {

    /**
     * Detects the phone's active local IPv4 address (Wi-Fi or Hotspot).
     * Strictly excludes loopbacks, link-local (169.254.x.x), cellular, and virtual interfaces.
     */
    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            val candidates = mutableListOf<Pair<String, String>>()

            while (interfaces.hasMoreElements()) {
                val netIface = interfaces.nextElement()

                // 1. Skip interfaces that are down, loopbacks, or point-to-point
                if (!netIface.isUp || netIface.isLoopback || netIface.isPointToPoint) continue

                val name = netIface.name.lowercase()

                // 2. Exclude loopback, virtual/VPN tunnels, and cellular data interfaces
                if (name.startsWith("lo") ||
                    name.startsWith("dummy") ||
                    name.startsWith("tun") ||
                    name.startsWith("rmnet") ||
                    name.startsWith("ccmni") ||
                    name.startsWith("pdp")) continue

                val addresses = netIface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val inetAddr = addresses.nextElement()

                    // FIXED: Changed inetAddr.isLoopback to inetAddr.isLoopbackAddress
                    if (!inetAddr.isLoopbackAddress && inetAddr is Inet4Address) {
                        val ip = inetAddr.hostAddress ?: continue

                        // 3. Exclude 127.*, 0.0.0.0, and Link-Local (169.254.*)
                        if (ip.startsWith("127.") || ip == "0.0.0.0" || ip.startsWith("169.254.")) continue

                        // 4. Restrict to private LAN IP ranges (RFC 1918)
                        if (isPrivateSubnet(ip)) {
                            candidates.add(Pair(name, ip))
                        }
                    }
                }
            }

            // 5. Prioritize active Wi-Fi / Hotspot interfaces across Android OEMs
            val preferred = candidates.firstOrNull { (name, _) ->
                name.contains("wlan") ||
                        name.contains("ap") ||
                        name.contains("softap") ||
                        name.contains("rndis") ||
                        name.contains("p2p")
            }

            return preferred?.second ?: candidates.firstOrNull()?.second
        } catch (e: Exception) {
            return null
        }
    }

    private fun isPrivateSubnet(ip: String): Boolean {
        return ip.startsWith("192.168.") ||
                ip.startsWith("10.") ||
                (ip.startsWith("172.") && ip.split(".").getOrNull(1)?.toIntOrNull() in 16..31)
    }
}