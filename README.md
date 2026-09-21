# CloudTerm Android

Cliente nativo para Android (Kotlin + Jetpack Compose) con la identidad visual
de [CloudTerm](https://github.com/pilahito/CloudTerm) de escritorio.

Protocolos: **SSH**, **SFTP**, **FTP** y **FTPS**. Editor de código remoto.

## Compilar el APK

### En GitHub Actions (recomendado)

1. Actions → *APK de Android* → **Run workflow** (rama `feat/agents-2fa-highlight` o `main`).
2. Descarga el artefacto `CloudTerm-Android-debug`.

```bash
./gradlew :app:assembleDebug
```

## Qué incluye

- Protocolo al crear el servidor: SSH (22 + terminal), SFTP (22 archivos),
  FTP (21) y FTPS (990 implícito / 21 explícito).
- Terminal xterm.js solo en SSH. FTP/FTPS no tienen shell; usan Archivos y Código.
- Editor visual: pestaña **Código** con resaltado token a token (keywords, strings, comentarios, números, funciones).
- **Pixel Agents**: pestaña Agents narra cada transferencia (qué archivo se mueve).
- **Asistente IA** con detector de tarea y asignación de modelo.
- **2FA TOTP**: secreto en el Keystore; se responde el prompt keyboard-interactive.
- **Detector de huella**: huella SSH del host al conectar + desbloqueo biométrico de la app (botón Huella ON).
- Credenciales AES-GCM + Android Keystore. No se publican logs del servidor.

Versión **1.2.0**.
