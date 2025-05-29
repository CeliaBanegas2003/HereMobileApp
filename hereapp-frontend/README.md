# HereApp

HereApp es una aplicación móvil desarrollada en Flutter para la gestión de asistencia mediante tarjetas NFC. Proporciona una experiencia de usuario con estilo iOS en cualquier dispositivo y permite tanto a usuarios normal como a administradores interactuar con el sistema de forma sencilla.

## Funcionalidades principales

* **Autenticación de usuarios**: Inicio de sesión mediante correo y contraseña.
* **Lectura NFC**: Registro de entrada y salida de asistencia al acercar la tarjeta NFC.
* **Interfaz iOS**: Diseño unificado usando tipografía y componentes al estilo iOS, tanto en Android como en iOS.
* **Registro de tarjetas (solo administradores)**: Permite dar de alta nuevas tarjetas NFC en el sistema.
* **Notificaciones en pantalla**: Mensajes de éxito, error y advertencia con estilo iOS.

## Requisitos previos

* **Flutter SDK** (canal estable)
* **Android SDK**
* **Dispositivo Android** o emulador con NFC (para pruebas en NFC real)
* **Backend** desplegado y accesible desde la red local (configurar la URL en el código fuente)

## Instalación y uso con APK

1. **Descargar el APK**

   Copia el archivo `app-release.apk` en tu dispositivo Android.

2. **Habilitar fuentes desconocidas**

   Ve a **Ajustes > Seguridad** y activa **Fuentes desconocidas** para permitir la instalación de la app.

3. **Instalar el APK**

   Desde el explorador de archivos de tu dispositivo, busca `app-release.apk` y pulsa para instalar.

4. **Ejecutar la aplicación**

   Abre HereApp, inicia sesión con tus credenciales y prueba la lectura NFC.

> **Nota:** Con el APK la aplicación funciona plenamente, sin necesidad de herramientas de desarrollo.

## Estructura del proyecto

* `lib/`

    * `main.dart`: Punto de entrada, configura rutas y tema.
    * `nfcView.dart`: Lógica de lectura/registro de tarjetas NFC.
    * `assets/images/`: Imágenes de fondo y logotipos.

* `android/`
  Configuración de firma, keystore y build para Android.

## Licencia

Este proyecto está bajo la licencia MIT. Lee el fichero [LICENSE](LICENSE) para más detalles.
