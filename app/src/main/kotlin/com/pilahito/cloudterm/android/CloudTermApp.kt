package com.pilahito.cloudterm.android

import android.app.Application
import android.util.Log
import com.jcraft.jsch.JSch
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

class CloudTermApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instalarBouncyCastle()
        configurarJsch()
    }

    companion object {
        private const val TAG = "CloudTerm"

        fun instalarBouncyCastle() {
            // Android trae un BC recortado. Lo sustituimos por el completo
            // para que JSch pueda usar ed25519 en API 26–33.
            try {
                Security.removeProvider("BC")
            } catch (_: Exception) {
            }
            val provider = BouncyCastleProvider()
            if (Security.getProvider(provider.name) == null) {
                Security.insertProviderAt(provider, 1)
            }
            Log.i(TAG, "BouncyCastle ${provider.version} listo")
        }

        fun configurarJsch() {
            JSch.setConfig(
                "kex",
                "curve25519-sha256,curve25519-sha256@libssh.org,ecdh-sha2-nistp256," +
                    "ecdh-sha2-nistp384,ecdh-sha2-nistp521,diffie-hellman-group-exchange-sha256," +
                    "diffie-hellman-group16-sha512,diffie-hellman-group14-sha256",
            )
            JSch.setConfig(
                "server_host_key",
                "ssh-ed25519,ecdsa-sha2-nistp256,ecdsa-sha2-nistp384,ecdsa-sha2-nistp521," +
                    "rsa-sha2-512,rsa-sha2-256,ssh-rsa",
            )
            JSch.setConfig(
                "PubkeyAcceptedAlgorithms",
                "ssh-ed25519,ecdsa-sha2-nistp256,rsa-sha2-512,rsa-sha2-256,ssh-rsa",
            )
            JSch.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password")
            JSch.setConfig("HashKnownHosts", "no")
        }
    }
}
