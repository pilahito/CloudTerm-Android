# CloudTerm Android

Cliente **SSH y SFTP nativo** para Android (Kotlin + Jetpack Compose), con la
identidad visual de [CloudTerm](https://github.com/pilahito/CloudTerm) de escritorio.

Icono, paleta y nombre coinciden con la app de Tauri. El protocolo lo habla
**JSch** (fork mwiede) dentro del teléfono: no hace falta el NDK ni Rust.

## Compilar el APK

### En GitHub Actions (recomendado)

1. Este repositorio ya tiene el flujo **APK de Android**.
2. Actions → *APK de Android* → **Run workflow**.
3. Descarga el artefacto `CloudTerm-Android-debug` e instálalo.

También se adjunta a la Release al empujar una etiqueta `v*`.

### En el teléfono / con Android Studio

Abre esta carpeta en Android Studio (SDK 34, JDK 17) y pulsa Run.
O, con el SDK instalado:

```bash
./gradlew :app:assembleDebug
# APK en app/build/outputs/apk/debug/
```

## Qué incluye (estable)

- Lista de servidores con **contraseña o clave privada** (ed25519, ECDSA, RSA).
- Credenciales cifradas con **AES-GCM**; la clave vive en el **Android Keystore**.
- Aviso de huella del servidor la primera vez; rechazo si después cambia.
- Terminal **xterm.js** con teclas extra (Esc, Tab, Ctrl, flechas, Pegar, Copiar).
- El teclado en pantalla ignora el `keyCode 229` del IME para no duplicar letras.
- Archivos por SFTP: navegar, subir, descargar, renombrar, eliminar, crear carpetas.
- **Sesión en segundo plano**: un servicio en primer plano mantiene el SSH vivo.
- **BouncyCastle** para que ed25519 funcione en Android 8 (API 26), no solo en 14+.

## Iconos

Los mipmaps salen del icono maestro de escritorio (`docs/brand/icon-1024.png`):

```bash
python scripts/generar_iconos.py
```

También genera `store/icon-512.png` y `store/feature-graphic.png` para Play Store.

## Qué sigue pendiente

Pixel Agents, asistente IA, segundo factor y editar archivos remotos en el sitio
(el de escritorio sí los tiene).
