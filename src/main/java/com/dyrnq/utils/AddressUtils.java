package com.dyrnq.utils;

import io.netty.util.NetUtil;

import java.net.*;

import static java.util.Objects.requireNonNull;

public class AddressUtils {

    private AddressUtils() {
    }

    /**
     * Creates InetSocketAddress instance. Numeric IP addresses will be detected and
     * resolved without doing reverse DNS lookups.
     *
     * @param hostname ip-address or hostname
     * @param port     port number
     * @param resolve  when true, resolve given hostname at instance creation time
     * @return InetSocketAddress for given parameters
     */
    public static InetSocketAddress createInetSocketAddress(String hostname, int port, boolean resolve) {
        requireNonNull(hostname, "hostname");
        InetSocketAddress inetAddressForIpString = createForIpString(hostname, port);
        if (inetAddressForIpString != null) {
            return inetAddressForIpString;
        } else {
            return resolve ? new InetSocketAddress(hostname, port) : InetSocketAddress.createUnresolved(hostname, port);
        }
    }

    /**
     * Creates InetSocketAddress that is always resolved. Numeric IP addresses will be
     * detected and resolved without doing reverse DNS lookups.
     *
     * @param hostname ip-address or hostname
     * @param port     port number
     * @return InetSocketAddress for given parameters
     */
    public static InetSocketAddress createResolved(String hostname, int port) {
        return createInetSocketAddress(hostname, port, true);
    }

    /**
     * Creates unresolved InetSocketAddress. Numeric IP addresses will be detected and
     * resolved.
     *
     * @param hostname ip-address or hostname
     * @param port     port number
     * @return InetSocketAddress for given parameters
     */
    public static InetSocketAddress createUnresolved(String hostname, int port) {
        return createInetSocketAddress(hostname, port, false);
    }

    /**
     * Parse unresolved InetSocketAddress. Numeric IP addresses will be detected and resolved.
     *
     * @param address     ip-address or hostname
     * @param defaultPort the default port
     * @return {@link InetSocketAddress} for given parameters, only numeric IP addresses will be resolved
     */
    public static InetSocketAddress parseAddress(String address, int defaultPort) {
        return parseAddress(address, defaultPort, false);
    }

    /**
     * Parse unresolved InetSocketAddress. Numeric IP addresses will be detected and resolved.
     *
     * @param address     ip-address or hostname
     * @param defaultPort is used if the address does not contain a port,
     *                    or if the port cannot be parsed in non-strict mode
     * @param strict      if true throws an exception when the address cannot be parsed,
     *                    otherwise an unresolved {@link InetSocketAddress} is returned. It can include the case of the host
     *                    having been parsed but not the port (replaced by {@code defaultPort})
     * @return {@link InetSocketAddress} for given parameters, only numeric IP addresses will be resolved
     */
    public static InetSocketAddress parseAddress(String address, int defaultPort, boolean strict) {
        requireNonNull(address, "address");
        String host = address;
        int port = defaultPort;
        int separatorIdx = address.lastIndexOf(':');
        int ipV6HostSeparatorIdx = address.lastIndexOf(']');
        if (separatorIdx > ipV6HostSeparatorIdx) {
            if (separatorIdx == address.indexOf(':') || ipV6HostSeparatorIdx > -1) {
                host = address.substring(0, separatorIdx);
                String portStr = address.substring(separatorIdx + 1);
                if (!portStr.isEmpty()) {
                    if (portStr.chars().allMatch(Character::isDigit)) {
                        port = Integer.parseInt(portStr);
                    } else if (strict) {
                        throw new IllegalArgumentException("Failed to parse a port from " + address);
                    }
                }
            } else if (strict) {
                throw new IllegalArgumentException("Invalid IPv4 address " + address);
            }
        }
        return AddressUtils.createUnresolved(host, port);
    }

    /**
     * Replaces an unresolved InetSocketAddress with a resolved instance in the case that
     * the passed address is a numeric IP address (both IPv4 and IPv6 are supported).
     *
     * @param inetSocketAddress socket address instance to process
     * @return processed socket address instance
     */
    public static InetSocketAddress replaceUnresolvedNumericIp(InetSocketAddress inetSocketAddress) {
        requireNonNull(inetSocketAddress, "inetSocketAddress");
        if (!inetSocketAddress.isUnresolved()) {
            return inetSocketAddress;
        }
        InetSocketAddress inetAddressForIpString = createForIpString(
                inetSocketAddress.getHostString(), inetSocketAddress.getPort());
        if (inetAddressForIpString != null) {
            return inetAddressForIpString;
        } else {
            return inetSocketAddress;
        }
    }

    /**
     * Replaces an unresolved InetSocketAddress with a resolved instance in the case that
     * the passed address is unresolved.
     *
     * @param inetSocketAddress socket address instance to process
     * @return resolved instance with same host string and port
     */
    public static InetSocketAddress replaceWithResolved(InetSocketAddress inetSocketAddress) {
        requireNonNull(inetSocketAddress, "inetSocketAddress");
        if (!inetSocketAddress.isUnresolved()) {
            return inetSocketAddress;
        }
        inetSocketAddress = replaceUnresolvedNumericIp(inetSocketAddress);
        if (!inetSocketAddress.isUnresolved()) {
            return inetSocketAddress;
        } else {
            return new InetSocketAddress(inetSocketAddress.getHostString(), inetSocketAddress.getPort());
        }
    }


    static InetAddress attemptParsingIpString(String hostname) {
        byte[] ipAddressBytes = NetUtil.createByteArrayFromIpAddressString(hostname);

        if (ipAddressBytes != null) {
            try {
                if (ipAddressBytes.length == 4) {
                    return Inet4Address.getByAddress(ipAddressBytes);
                } else {
                    return Inet6Address.getByAddress(null, ipAddressBytes, -1);
                }
            } catch (UnknownHostException e) {
                throw new RuntimeException(e); // Should never happen
            }
        }

        return null;
    }

    static InetSocketAddress createForIpString(String hostname, int port) {
        InetAddress inetAddressForIpString = attemptParsingIpString(hostname);
        if (inetAddressForIpString != null) {
            return new InetSocketAddress(inetAddressForIpString, port);
        }
        return null;
    }
}
