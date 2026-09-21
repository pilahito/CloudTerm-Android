# CloudTerm Android

Cliente nativo para Android (Kotlin + Jetpack Compose) con la identidad visual
de [CloudTerm](https://github.com/pilahito/CloudTerm) de escritorio.

Protocolos: **SSH**, **SFTP**, **FTP** y **FTPS**. Editor de código remoto.

## Instalar el APK

Descarga **1.3.2** (no uses 1.2.0):
https://github.com/pilahito/CloudTerm-Android/releases/tag/v1.3.2

El 1.2.0 era un APK de depuración. Samsung responde "Aplicación no instalada"
si queda esa versión o si la firma no coincide.

1. Desinstala CloudTerm anterior si aparece en Ajustes → Apps.
2. En Archivos o Chrome permite instalar apps desconocidas.
3. Abre `CloudTerm-android-1.3.2.apk`.

## Compilar

Actions → *APK de Android* → **Run workflow** (rama `main`).

## Qué incluye

- Protocolo al crear el servidor: SSH (22 + terminal), SFTP (22 archivos),
  FTP (21) y FTPS (990 implícito / 21 explícito).
- Terminal xterm.js solo en SSH. FTP/FTPS no tienen shell; usan Archivos y Código.
- Editor visual: pestaña **Código** con resaltado token a token.
- **Pixel Agents**: narra cada transferencia.
- **Asistente IA** con detector de tarea.
- **2FA TOTP** y **detector de huella** (app + host SSH).
- Credenciales AES-GCM + Android Keystore. No se publican logs del servidor.

Versión **1.3.2** (`com.pilahito.cloudterm.mobile`).
