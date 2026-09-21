# CloudTerm Android

Cliente nativo para Android (Kotlin + Jetpack Compose) con la identidad visual
de [CloudTerm](https://github.com/pilahito/CloudTerm) de escritorio.

Protocolos: **SSH**, **SFTP**, **FTP** y **FTPS**. Editor de código remoto.

## Compilar el APK

### En GitHub Actions (recomendado)

1. Actions → *APK de Android* → **Run workflow**.
2. Descarga el artefacto `CloudTerm-Android-debug`.

```bash
./gradlew :app:assembleDebug
```

## Qué incluye

- Protocolo al crear el servidor: SSH (22 + terminal), SFTP (22 archivos),
  FTP (21) y FTPS (990 implícito / 21 explícito).
- Terminal xterm.js solo en SSH. FTP/FTPS no tienen shell; usan Archivos y Código.
- Editor visual: pestaña **Código**.
- Credenciales AES-GCM + Android Keystore.
- JSch + Commons Net.

## Tienda de plugins (sin nube propia)

CloudTerm **no hospeda** un marketplace. La pestaña Plugins habla con servidores
ya existentes:

| Catálogo | URL | Coste para nosotros |
|---|---|---|
| **Acode** (por defecto) | `https://acode.app/api/plugins` y `.../plugin/download/{id}` | Cero. Lo paga Acode. |
| Open VSX | `https://open-vsx.org/api` | Cero. Eclipse Foundation. |

Los zips/VSIX se guardan en el teléfono (`filesDir/plugins`). No hay CDN,
ni API key, ni factura de cloud por la tienda.

Los plugins de Acode se **ejecutan en Acode** (`acode.require`). CloudTerm solo
lista y descarga el catálogo. Temas/snippets de Open VSX sí se aplican aquí.

Si Acode cierra o limita el API público, habría que cambiar de backend; hasta
entonces no hace falta servidor nuestro.
