# CloudTerm Android

Cliente nativo (Kotlin + Jetpack Compose) con la misma identidad visual que
[CloudTerm de escritorio](https://github.com/pilahito/CloudTerm).

**SSH · SFTP · FTP · FTPS** · editor propio · Pixel Agents · asistente IA

## Descargar

| Archivo | Notas |
| --- | --- |
| [CloudTerm-android-1.3.3.apk](https://github.com/pilahito/CloudTerm-Android/releases/download/v1.3.3/CloudTerm-android-1.3.3.apk) | Release firmado. Android 8+ |
| [Todas las versiones](https://github.com/pilahito/CloudTerm-Android/releases) | No uses el 1.2.0 |

Paquete: `com.pilahito.cloudterm.mobile`

### Si sale «Aplicación no instalada»

1. Ajustes → Aplicaciones → CloudTerm → Desinstalar (cualquier versión anterior).
2. Archivos o Chrome → permitir instalar apps desconocidas.
3. Instala **1.3.3**. El 1.2.0 era debug y Samsung lo bloquea.

## Qué incluye

- **SSH** (puerto 22, terminal xterm.js), **SFTP**, **FTP** (21) y **FTPS** (990 / explícito).
- Editor en la pestaña Código, resaltado token a token.
- **Pixel Agents**: mensaje de qué archivo se está moviendo.
- **IA** con detector de tarea y modelos básicos según el hardware.
- **2FA TOTP** en keyboard-interactive y **huella** (app + fingerprint SSH).
- Contraseñas en Android Keystore (AES-GCM). Nada de logs del servidor en el repo.

## Compilar

Actions → *APK de Android* → **Run workflow** (`main`).

```bash
gradle :app:assembleRelease
```

El keystore de Releases vive en `store/cloudterm.jks.b64`.
La clave de Google Play, cuando exista, no se sube al repo.

## Escritorio

Windows `.exe`, Linux `.deb` / AppImage: [CloudTerm 1.0.5](https://github.com/pilahito/CloudTerm/releases/tag/v1.0.5)
