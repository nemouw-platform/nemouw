package com.nemouw.common.util;

import com.nemouw.common.util.UserAgentParser.Result;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class UserAgentParserTest {

    private static final Result UNKNOWN = new Result("unknown", "Unknown", "Unknown");
    private static final String CHROME_ENGINE = "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0";
    private static final String CHROME_ON_WINDOWS =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " + CHROME_ENGINE + " Safari/537.36";
    private static final String CHROME_ON_MACOS =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " + CHROME_ENGINE + " Safari/537.36";
    private static final String SAFARI_ON_MACOS =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) "
                    + "Version/17.2 Safari/605.1.15";
    private static final String OPERA_ON_WINDOWS = CHROME_ON_WINDOWS + " OPR/106.0.0.0";
    private static final String CUBOT_UNDERSCORE = "Mozilla/5.0 (Linux; Android 12; CUBOT_KINGKONG_5 "
            + "Build/SP1A.210812.016) " + CHROME_ENGINE + " Mobile Safari/537.36";
    private static final String CUBOT_SPACED =
            "Mozilla/5.0 (Linux; Android 13; CUBOT P80) " + CHROME_ENGINE + " Mobile Safari/537.36";

    private static Result result(String device, String os, String browser) {
        return new Result(device, os, browser);
    }

    static Stream<Arguments> clients() {
        return Stream.of(
                arguments("Chrome on Windows", CHROME_ON_WINDOWS, "desktop", "Windows", "Chrome"),
                arguments("Chrome on Linux", "Mozilla/5.0 (X11; Linux x86_64) " + CHROME_ENGINE + " Safari/537.36",
                        "desktop", "Linux", "Chrome"),
                arguments("Chromium on Ubuntu", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64) AppleWebKit/537.36 "
                                + "(KHTML, like Gecko) Ubuntu Chromium/120.0.0.0 Chrome/120.0.0.0 Safari/537.36",
                        "desktop", "Linux", "Chrome"),
                arguments("Chrome on ChromeOS", "Mozilla/5.0 (X11; CrOS x86_64 14541.0.0) " + CHROME_ENGINE
                        + " Safari/537.36", "desktop", "ChromeOS", "Chrome"),
                arguments("Firefox on Linux", "Mozilla/5.0 (X11; Linux x86_64; rv:121.0) Gecko/20100101 Firefox/121.0",
                        "desktop", "Linux", "Firefox"),
                arguments("Edge on Windows", CHROME_ON_WINDOWS + " Edg/120.0.0.0", "desktop", "Windows", "Edge"),
                arguments("Edge on Android", "Mozilla/5.0 (Linux; Android 13; Pixel 7) " + CHROME_ENGINE
                        + " Mobile Safari/537.36 EdgA/120.0.0.0", "mobile", "Android", "Edge"),
                arguments("Edge on iOS", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) "
                        + "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 EdgiOS/120.0.2210.126 "
                        + "Mobile/15E148 Safari/604.1", "mobile", "iOS", "Edge"),
                arguments("Legacy Edge", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                                + "(KHTML, like Gecko) Chrome/70.0.3538.102 Safari/537.36 Edge/18.19582",
                        "desktop", "Windows", "Edge"),
                arguments("Legacy Opera", "Opera/9.80 (Windows NT 6.1; U; en) Presto/2.12.388 Version/12.16",
                        "desktop", "Windows", "Opera"),
                arguments("Samsung Internet beats Chrome", "Mozilla/5.0 (Linux; Android 13; SM-S918B) "
                        + "AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/23.0 Chrome/115.0.0.0 "
                        + "Mobile Safari/537.36", "mobile", "Android", "Samsung Internet"),
                arguments("Firefox on iOS", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) "
                                + "AppleWebKit/605.1.15 (KHTML, like Gecko) FxiOS/121.0 Mobile/15E148 Safari/605.1.15",
                        "mobile", "iOS", "Firefox"),
                arguments("Chrome on iOS", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) "
                                + "AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/120.0.6099.119 Mobile/15E148 Safari/604.1",
                        "mobile", "iOS", "Chrome"),
                arguments("Safari on iPhone", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) "
                                + "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1",
                        "mobile", "iOS", "Safari"),
                arguments("Safari on iPod", "Mozilla/5.0 (iPod touch; CPU iPhone OS 12_0 like Mac OS X) "
                                + "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/12.0 Mobile/15E148 Safari/604.1",
                        "mobile", "iOS", "Safari"),
                arguments("Safari on a legacy iPad", "Mozilla/5.0 (iPad; CPU OS 12_2 like Mac OS X) "
                                + "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/12.1 Mobile/15E148 Safari/604.1",
                        "tablet", "iOS", "Safari"),
                arguments("Chrome on an Android phone", "Mozilla/5.0 (Linux; Android 13; Pixel 7) " + CHROME_ENGINE
                        + " Mobile Safari/537.36", "mobile", "Android", "Chrome"),
                arguments("Chrome on an Android tablet without the Mobile token", "Mozilla/5.0 (Linux; Android 13; "
                        + "SM-X700) " + CHROME_ENGINE + " Safari/537.36", "tablet", "Android", "Chrome"),
                arguments("Tablet in the device name wins over Mobile", "Mozilla/5.0 (Linux; Android 9; Tablet) "
                        + CHROME_ENGINE + " Mobile Safari/537.36", "tablet", "Android", "Chrome"),
                arguments("Unknown OS with a Mobile token", "SomeApp/1.0 (Mobile)", "mobile", "Unknown", "Unknown"),
                arguments("curl", "curl/8.4.0", "unknown", "Unknown", "curl"),
                arguments("Postman", "PostmanRuntime/7.36.0", "unknown", "Unknown", "Postman"));
    }

    static Stream<Arguments> bots() {
        return Stream.of(
                arguments("Googlebot", "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"),
                arguments("bingbot", "Mozilla/5.0 (compatible; bingbot/2.0; +http://www.bing.com/bingbot.htm)"),
                arguments("YandexBot", "Mozilla/5.0 (compatible; YandexBot/3.0; +http://yandex.com/bots)"),
                arguments("DuckDuckBot", "DuckDuckBot/1.1; (+http://duckduckgo.com/duckduckbot.html)"),
                arguments("AhrefsBot", "Mozilla/5.0 (compatible; AhrefsBot/7.0; +http://ahrefs.com/robot/)"),
                arguments("PetalBot, with bot followed by a semicolon",
                        "Mozilla/5.0 (compatible; PetalBot;+https://webmaster.petalsearch.com/site/petalbot)"),
                arguments("Slackbot, with bot followed by a hyphen",
                        "Slackbot-LinkExpanding 1.0 (+https://api.slack.com/robots)"),
                arguments("Baiduspider",
                        "Mozilla/5.0 (compatible; Baiduspider/2.0; +http://www.baidu.com/search/spider.html)"),
                arguments("Yahoo Slurp",
                        "Mozilla/5.0 (compatible; Yahoo! Slurp; http://help.yahoo.com/help/us/ysearch/slurp)"),
                arguments("A generic crawler", "acme-crawler/2.0"),
                arguments("A standalone Bot token", "Mozilla/5.0 (compatible; Bot/1.0)"),
                arguments("Uppercase Googlebot", "MOZILLA/5.0 (COMPATIBLE; GOOGLEBOT/2.1)"));
    }

    @Nested
    class HappyPaths {

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.nemouw.common.util.UserAgentParserTest#clients")
        void classifiesCommonClients(String label, String userAgent, String device, String os, String browser) {
            assertThat(UserAgentParser.parse(userAgent)).isEqualTo(result(device, os, browser));
        }
    }

    @Nested
    class PrecedenceRules {

        @Test
        void detectsOperaEvenThoughItsUserAgentAlsoSaysChromeAndSafari() {
            assertThat(OPERA_ON_WINDOWS).contains("Chrome/", "Safari/", "OPR/");

            assertThat(UserAgentParser.parse(OPERA_ON_WINDOWS)).isEqualTo(result("desktop", "Windows", "Opera"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"Edg/120.0.0.0", "EdgA/120.0.0.0", "EdgiOS/120.0.0.0", "Edge/18.19582"})
        void detectsEdgeEvenThoughItsUserAgentAlsoSaysChromeAndSafari(String edgeToken) {
            String userAgent = CHROME_ON_WINDOWS + " " + edgeToken;

            assertThat(UserAgentParser.parse(userAgent).browser()).isEqualTo("Edge");
        }

        @Test
        void detectsDesktopChromeAsChromeEvenThoughItsUserAgentSaysSafari() {
            assertThat(CHROME_ON_MACOS).contains("Safari/537.36");

            assertThat(UserAgentParser.parse(CHROME_ON_MACOS)).isEqualTo(result("desktop", "macOS", "Chrome"));
        }

        @Test
        void detectsMacSafariAsSafariWhenThereIsNoChromeToken() {
            assertThat(SAFARI_ON_MACOS).doesNotContain("Chrome/").contains("Version/17.2 Safari/");

            assertThat(UserAgentParser.parse(SAFARI_ON_MACOS)).isEqualTo(result("desktop", "macOS", "Safari"));
        }

        @Test
        void reportsAnIpadOsDeviceAsMacOsDesktopBecauseItSendsAMacintoshUserAgent() {
            assertThat(UserAgentParser.parse(SAFARI_ON_MACOS).osName()).isEqualTo("macOS");
            assertThat(UserAgentParser.parse(SAFARI_ON_MACOS).deviceType()).isEqualTo("desktop");
        }

        @Test
        void prefersAndroidOverTheLinuxTokenInItsUserAgent() {
            String userAgent = "Mozilla/5.0 (Linux; Android 13; Pixel 7) " + CHROME_ENGINE + " Mobile Safari/537.36";

            assertThat(userAgent).contains("Linux");
            assertThat(UserAgentParser.parse(userAgent).osName()).isEqualTo("Android");
        }

        @Test
        void prefersIosOverTheMacOsXTokenInItsUserAgent() {
            String userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15 "
                    + "(KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1";

            assertThat(userAgent).contains("Mac OS X");
            assertThat(UserAgentParser.parse(userAgent).osName()).isEqualTo("iOS");
        }

        @Test
        void prefersChromeOsOverTheX11TokenInItsUserAgent() {
            String userAgent = "Mozilla/5.0 (X11; CrOS x86_64 14541.0.0) " + CHROME_ENGINE + " Safari/537.36";

            assertThat(userAgent).contains("X11");
            assertThat(UserAgentParser.parse(userAgent).osName()).isEqualTo("ChromeOS");
        }

        @Test
        void prefersTheBotLabelOverAndroidAndMobile() {
            String googlebotSmartphone = "Mozilla/5.0 (Linux; Android 6.0.1; Nexus 5X Build/MMB29P) " + CHROME_ENGINE
                    + " Mobile Safari/537.36 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)";

            assertThat(UserAgentParser.parse(googlebotSmartphone)).isEqualTo(result("bot", "Android", "Chrome"));
        }
    }

    @Nested
    class BotDetection {

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.nemouw.common.util.UserAgentParserTest#bots")
        void detectsBots(String label, String userAgent) {
            assertThat(UserAgentParser.parse(userAgent).deviceType()).isEqualTo("bot");
        }

        @ParameterizedTest
        @ValueSource(strings = {CUBOT_UNDERSCORE, CUBOT_SPACED})
        void doesNotMistakeCubotPhonesForBots(String userAgent) {
            assertThat(UserAgentParser.parse(userAgent)).isEqualTo(result("mobile", "Android", "Chrome"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"DuckDuckGo/5", "baidubrowser/4.14"})
        void doesNotMistakeBrowsersWithBotLikeNamesForBots(String browserToken) {
            String userAgent = "Mozilla/5.0 (Linux; Android 13; Pixel 7) " + CHROME_ENGINE
                    + " Mobile Safari/537.36 " + browserToken;

            assertThat(UserAgentParser.parse(userAgent)).isEqualTo(result("mobile", "Android", "Chrome"));
        }
    }

    @Nested
    class BoundaryValues {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        void returnsUnknownForNullEmptyAndBlank(String userAgent) {
            assertThat(UserAgentParser.parse(userAgent)).isEqualTo(UNKNOWN);
        }

        @Test
        void detectsATokenThatEndsExactlyAtTheLastKeptCharacter() {
            String browserToken = "x".repeat(504) + "Firefox/";
            String osToken = "x".repeat(505) + "Windows";

            assertThat(browserToken).hasSize(512);
            assertThat(osToken).hasSize(512);
            assertThat(UserAgentParser.parse(browserToken).browser()).isEqualTo("Firefox");
            assertThat(UserAgentParser.parse(osToken).osName()).isEqualTo("Windows");
        }

        @Test
        void ignoresATokenCutOffByItsLastCharacter() {
            String browserToken = "x".repeat(505) + "Firefox/";
            String osToken = "x".repeat(506) + "Windows";

            assertThat(browserToken).hasSize(513);
            assertThat(osToken).hasSize(513);
            assertThat(UserAgentParser.parse(browserToken)).isEqualTo(UNKNOWN);
            assertThat(UserAgentParser.parse(osToken)).isEqualTo(UNKNOWN);
        }

        @Test
        void ignoresAWholeTokenBeyondTheTruncation() {
            assertThat(UserAgentParser.parse("x".repeat(600) + "Firefox/121.0")).isEqualTo(UNKNOWN);
        }

        @Test
        void handlesAVeryLongUserAgentWithoutFailing() {
            assertThat(UserAgentParser.parse(CHROME_ON_WINDOWS + "x".repeat(10_000)))
                    .isEqualTo(result("desktop", "Windows", "Chrome"));
            assertThat(UserAgentParser.parse("x".repeat(10_000))).isEqualTo(UNKNOWN);
        }
    }

    @Nested
    class ErrorPaths {

        @ParameterizedTest
        @ValueSource(strings = {"definitely not a user agent", "12345", "???"})
        void returnsUnknownForTextThatIsNotAUserAgent(String userAgent) {
            assertThat(UserAgentParser.parse(userAgent)).isEqualTo(UNKNOWN);
        }

        @Test
        void doesNotRecogniseLowercaseBrowserAndOsTokens() {
            String userAgent = "mozilla/5.0 (windows nt 10.0) chrome/120.0 safari/537.36";

            assertThat(UserAgentParser.parse(userAgent)).isEqualTo(UNKNOWN);
        }
    }
}