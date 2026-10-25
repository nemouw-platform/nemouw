package com.nemouw.common.web;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

import static com.nemouw.support.Fixtures.props;
import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private static final String REAL_CLIENT = "198.51.100.9";
    private static final String PUBLIC_PEER = "203.0.113.7";
    private static final String NGINX_PEER = "10.0.4.12";

    @Nested
    class HappyPaths {

        @Test
        void usesTheConnectionAddressWhenNoProxyIsTrusted() {
            String ip = ClientIpResolver.resolve(PUBLIC_PEER, REAL_CLIENT, false);

            assertThat(ip).isEqualTo(PUBLIC_PEER);
        }

        @ParameterizedTest
        @ValueSource(strings = {"127.0.0.1", "10.0.4.12", "172.16.8.3", "192.168.1.20", "::1", "fd12:3456:789a::1", "fe80::1"})
        void usesTheLastForwardedAddressWhenTheConnectionComesFromAPrivateProxy(String proxy) {
            String ip = ClientIpResolver.resolve(proxy, REAL_CLIENT, true);

            assertThat(ip).isEqualTo(REAL_CLIENT);
        }

        @Test
        void ignoresAClientSuppliedFirstHopAndTrustsOnlyTheEntryTheProxyAppended() {
            String ip = ClientIpResolver.resolve(NGINX_PEER, "1.2.3.4, " + REAL_CLIENT, true);

            assertThat(ip).isEqualTo(REAL_CLIENT);
        }

        @Test
        void resolvesFromAServletRequestUsingTheTrustProxySetting() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr(NGINX_PEER);
            request.addHeader("X-Forwarded-For", REAL_CLIENT);

            assertThat(new ClientIpResolver(props(true, true)).resolve(request)).isEqualTo(REAL_CLIENT);
            assertThat(new ClientIpResolver(props(true, false)).resolve(request)).isEqualTo(NGINX_PEER);
        }
    }

    @Nested
    class BoundaryValues {

        @ParameterizedTest
        @ValueSource(strings = {"172.16.0.1", "172.31.255.255"})
        void treatsTheEdgesOfThePrivate172RangeAsProxies(String proxy) {
            assertThat(ClientIpResolver.resolve(proxy, REAL_CLIENT, true)).isEqualTo(REAL_CLIENT);
        }

        @ParameterizedTest
        @ValueSource(strings = {"172.15.255.255", "172.32.0.1", PUBLIC_PEER})
        void doesNotTreatAddressesJustOutsidePrivateRangesAsProxies(String peer) {
            assertThat(ClientIpResolver.resolve(peer, "1.2.3.4", true)).isEqualTo(peer);
        }

        @Test
        void normalizesAnIpv6ClientToItsFullHexForm() {
            assertThat(ClientIpResolver.resolve("2001:db8::7", null, false)).isEqualTo("2001:db8:0:0:0:0:0:7");
        }

        @Test
        void dropsAnIpv6ZoneIdentifier() {
            assertThat(ClientIpResolver.resolve("fe80::1%eth0", null, false)).isEqualTo("fe80:0:0:0:0:0:0:1");
        }

        @Test
        void unwrapsAnIpv4MappedIpv6Address() {
            assertThat(ClientIpResolver.resolve("::ffff:203.0.113.7", null, false)).isEqualTo(PUBLIC_PEER);
        }

        @Test
        void keepsTheConnectionAddressWhenTheHeaderIsMissingOrBlank() {
            assertThat(ClientIpResolver.resolve(NGINX_PEER, null, true)).isEqualTo(NGINX_PEER);
            assertThat(ClientIpResolver.resolve(NGINX_PEER, "   ", true)).isEqualTo(NGINX_PEER);
        }
    }

    @Nested
    class ErrorPaths {

        @Test
        void ignoresTheForwardedHeaderWhenTheConnectionIsFromAPublicAddressBecauseItCouldBeForged() {
            assertThat(ClientIpResolver.resolve(PUBLIC_PEER, REAL_CLIENT, true)).isEqualTo(PUBLIC_PEER);
        }

        @ParameterizedTest
        @ValueSource(strings = {"unknown", "evil.example.com", "999.1.1.1", "198.51.100.9, <script>", "1.2.3.4,   "})
        void fallsBackToTheConnectionAddressWhenTheLastForwardedEntryIsNotAnIpLiteral(String header) {
            assertThat(ClientIpResolver.resolve(NGINX_PEER, header, true)).isEqualTo(NGINX_PEER);
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "app.example.com", "010.0.0.1", "256.1.1.1", "1.2.3"})
        void returnsNullWhenTheConnectionAddressIsNotAValidIpLiteralAndNeverDoesADnsLookup(String peer) {
            assertThat(ClientIpResolver.resolve(peer, REAL_CLIENT, true)).isNull();
        }
    }
}
