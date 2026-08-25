package com.htoms.brief.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class GitHubReleaseParserTest {
    @Test
    fun parsesOnlyAllowedNewerApkWithSha256Digest() {
        val release = GitHubReleaseParser.parse(
            payload = payload(),
            currentVersionName = "2.0.0",
            currentVersionCode = 340515
        )

        requireNotNull(release)
        assertEquals("android-v2.1.0", release.tag)
        assertEquals("2.1.0", release.versionName)
        assertEquals(340964L, release.versionCode)
        assertEquals("a".repeat(64), release.sha256)
    }

    @Test
    fun returnsNullWhenReleaseIsNotNewerThanCurrentApp() {
        assertNull(
            GitHubReleaseParser.parse(
                payload = payload(),
                currentVersionName = "2.1.0",
                currentVersionCode = 340964
            )
        )
    }

    @Test
    fun rejectsWrongRepositoryPathAndMissingDigest() {
        assertThrows(UpdateMetadataException::class.java) {
            GitHubReleaseParser.parse(
                payload = payload().replace("/armsone/HtOMS-BK/", "/someone/other/"),
                currentVersionName = "2.0.0",
                currentVersionCode = 340515
            )
        }
        assertThrows(UpdateMetadataException::class.java) {
            GitHubReleaseParser.parse(
                payload = payload().replace("sha256:${"a".repeat(64)}", "sha512:${"a".repeat(64)}"),
                currentVersionName = "2.0.0",
                currentVersionCode = 340515
            )
        }
    }

    @Test
    fun rejectsPrereleaseAndMismatchedProductVersion() {
        assertThrows(UpdateMetadataException::class.java) {
            GitHubReleaseParser.parse(
                payload = payload().replace("\"prerelease\": false", "\"prerelease\": true"),
                currentVersionName = "2.0.0",
                currentVersionCode = 340515
            )
        }
        assertThrows(UpdateMetadataException::class.java) {
            GitHubReleaseParser.parse(
                payload = payload().replace("android-v2.1.0", "android-v2.1.1"),
                currentVersionName = "2.0.0",
                currentVersionCode = 340515
            )
        }
    }

    @Test
    fun rejectsMissingRequiredBodyMetadata() {
        assertThrows(UpdateMetadataException::class.java) {
            GitHubReleaseParser.parse(
                payload = payload().replace("Android-Version-Code: 340964\\n", ""),
                currentVersionName = "2.0.0",
                currentVersionCode = 340515
            )
        }
        assertThrows(UpdateMetadataException::class.java) {
            GitHubReleaseParser.parse(
                payload = payload().replace("Build-Number: 202608251843\\n", ""),
                currentVersionName = "2.0.0",
                currentVersionCode = 340515
            )
        }
    }

    private fun payload(): String = """
        {
          "tag_name": "android-v2.1.0",
          "draft": false,
          "prerelease": false,
          "body": "Android-Version-Code: 340964\nBuild-Number: 202608251843\n\n안전한 업데이트",
          "assets": [
            {
              "name": "HtOMS-Brief-Android-2.1.0.apk",
              "browser_download_url": "https://github.com/armsone/HtOMS-BK/releases/download/android-v2.1.0/HtOMS-Brief-Android-2.1.0.apk",
              "size": 123456,
              "digest": "sha256:${"a".repeat(64)}"
            }
          ]
        }
    """.trimIndent()
}
