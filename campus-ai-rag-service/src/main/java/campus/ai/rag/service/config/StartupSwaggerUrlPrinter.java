package campus.ai.rag.service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

@Component
public class StartupSwaggerUrlPrinter {

    private static final Logger log = LoggerFactory.getLogger(StartupSwaggerUrlPrinter.class);

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @EventListener(ApplicationReadyEvent.class)
    public void printSwaggerUrl() {
        String localIp = resolveLocalIpv4();
        String normalizedContextPath = normalizeContextPath(contextPath);
        String swaggerUrl = "http://" + localIp + ":" + serverPort + normalizedContextPath + "/doc.html";
        String openApiUrl = "http://" + localIp + ":" + serverPort + normalizedContextPath + "/v3/api-docs";

        log.info("----------------------------------------------------------");
        log.info("\tSwagger文档:\t{}", swaggerUrl);
        log.info("\tOpenAPI地址:\t{}", openApiUrl);
        log.info("----------------------------------------------------------");
    }

    private String resolveLocalIpv4() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress() && !address.isLinkLocalAddress()) {
                        String hostAddress = address.getHostAddress();
                        // 仅返回IPv4格式地址，避免打印 ::1 或 IPv6
                        if (hostAddress != null && hostAddress.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                            return hostAddress;
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            log.warn("获取本机IPv4失败，回退为 localhost，原因: {}", ex.getMessage());
        }
        return "127.0.0.1";
    }

    private String normalizeContextPath(String value) {
        if (value == null || value.trim().isEmpty() || "/".equals(value.trim())) {
            return "";
        }
        String trim = value.trim();
        return trim.startsWith("/") ? trim : "/" + trim;
    }
}

