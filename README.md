# CloudTerm Android

Cliente nativo para Android (Kotlin + Jetpack Compose) con la identidad visual
de [CloudTerm](https://github.com/pilahito/CloudTerm) de escritorio.

Protocolos: **SSH**, **SFTP**, **FTP** y **FTPS**. Editor de código remoto.

## Compilar el APK

### En GitHub Actions (recomendado)

1. Actions → *APK de Android* → **Run workflow** (rama `fix/ssh-real` o `main`).
2. Descarga el artefacto `CloudTerm-Android-debug`.

```bash
./gradlew :app:assembleDebug
```

## Qué incluye

- Protocolo al crear el servidor: SSH (22 + terminal), SFTP (22 archivos),
  FTP (21) y FTPS (990 implícito / 21 explícito).
- Terminal xterm.js solo en SSH. FTP/FTPS no tienen shell; usan Archivos y Código.
- Editor visual: pestaña **Código**. Toca un `.kt`, `.js`, `.py`, `.html`…
  números de línea, tema oscuro, Guardar escribe en el servidor.
- Credenciales AES-GCM + Android Keystore.
- JSch (keyboard-interactive) + Commons Net para FTP/FTPS.

## Pendiente

Pixel Agents, asistente IA, 2FA, resaltado token-a-token tipo VS Code.
