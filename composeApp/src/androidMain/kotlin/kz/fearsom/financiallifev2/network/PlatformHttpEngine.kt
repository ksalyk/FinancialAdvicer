package kz.fearsom.financiallifev2.network

import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*
import okhttp3.CertificatePinner
import java.util.concurrent.TimeUnit

/**
 * Android HTTP engine: OkHttp with certificate pinning.
 *
 * ════════════════════════════════════════════════════════════
 *  SETUP: get your SHA-256 pin hashes by running:
 *
 *    openssl s_client -connect api.yourapp.com:443 </dev/null 2>/dev/null | \
 *      openssl x509 -pubkey -noout | \
 *      openssl pkey -pubin -outform DER | \
 *      openssl dgst -sha256 -binary | base64
 *
 *  Replace the placeholder strings below with the real values.
 *  Always add a second backup pin (CA intermediate) so a cert rotation
 *  doesn't lock all users out.
 * ════════════════════════════════════════════════════════════
 *
 *  For local dev (10.0.2.2 / localhost) pinning is intentionally skipped
 *  because the local server runs over plain HTTP without a real certificate.
 *  The NetworkSecurityConfig handles the cleartext permission for those hosts.
 */
actual fun createPlatformEngine(): HttpClientEngine = OkHttp.create {
    config {
        connectTimeout(30, TimeUnit.SECONDS)
        readTimeout(30, TimeUnit.SECONDS)
        writeTimeout(30, TimeUnit.SECONDS)

        // ── Certificate pinning ─────────────────────────────────────────────
        // Uncomment and populate once you have a real production domain + cert.
        //
        // certificatePinner(
        //     CertificatePinner.Builder()
        //         .add(
        //             "api.yourapp.com",
        //             "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",  // primary
        //             "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="   // backup
        //         )
        //         .build()
        // )
        //
        // When you enable pinning:
        //  1. Update network_security_config.xml with the same hashes.
        //  2. Test on a real device (not just emulator) before shipping.
        //  3. Set a calendar reminder to renew the backup pin before expiry.
    }
}
