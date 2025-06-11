## 📱 Instalación de la Aplicación Móvil

### Instalación desde GitHub Releases (Recomendado)

#### Requisitos para Android
- Android 7.0 (API nivel 24) o superior
- Soporte NFC habilitado
- Al menos 100 MB de espacio libre

#### Descarga e Instalación

1. **Descargar la última versión**
    - Ve a [Releases](https://github.com/CeliaBanegas2003/HereMobileApp/releases)
    - Descarga **HereMobileApp v34** (Latest)
    - Busca el archivo APK en los assets de la release

2. **Habilitar instalación de fuentes desconocidas**
```bash
Configuración > Seguridad > Fuentes desconocidas
```

*O en versiones más recientes de Android:*
```bash
Configuración > Aplicaciones > Acceso especial > Instalar apps desconocidas
```

3. **Instalar la aplicación**
- Abrir el archivo APK descargado
- Tocar "Instalar" y seguir las instrucciones
- Conceder permisos de NFC cuando se solicite

4. **Verificar configuración NFC**
```bash
Configuración > Conexiones > NFC (debe estar habilitado)
```

#### ✅ Backend Pre-configurado

**El sistema ya está completamente operativo:** El backend está desplegado y funcionando en producción. Solo necesitas:

1. **Descargar e instalar el APK** desde las releases
2. **Abrir la aplicación** e iniciar sesión con credenciales válidas
3. **Comenzar a usar** el sistema inmediatamente

**URL del Backend:** `[https://hereapp-backend.onrender.com](https://backend-lectornfc-0-0-1-snapshot.onrender.com)`
- ✅ Base de datos configurada
- ✅ APIs REST operativas
- ✅ Sistema de autenticación activo
- ✅ Sincronización automática habilitada

#### Credenciales de Prueba

Para probar la aplicación, contacta con el administrador del sistema para obtener credenciales de acceso, o utiliza las credenciales proporcionadas durante la demostración del proyecto.

#### Verificación de Instalación

1. **Abrir la aplicación** HereMobileApp
2. **Introducir credenciales** de profesor/administrador
3. **Verificar conexión** - debería mostrar el dashboard principal
4. **Probar funcionalidad NFC** acercando una tarjeta MIFARE

### Instalación en iOS con Xcode

#### ✅ Funcionalidad Básica Disponible sin Licencia

La aplicación está configurada para funcionar en iOS **sin requerir Apple Developer Program**. Aunque las funcionalidades NFC estarán deshabilitadas, puedes:

- ✅ **Hacer login** con credenciales válidas
- ✅ **Navegar por la interfaz** de la aplicación
- ✅ **Probar el flujo de usuario** completo
- ✅ **Verificar la conectividad** con el backend
- ❌ **NFC deshabilitado** (requiere licencia de desarrollador)

#### Requisitos Mínimos
- macOS 12.0+ con Xcode 14.0+
- iOS 11.0+ en el dispositivo o simulador
- **No requiere** cuenta de Apple Developer para funcionalidad básica

#### Instalación y Prueba

1. **Clonar el repositorio**
   ```bash
   git clone https://github.com/CeliaBanegas2003/HereMobileApp.git
   cd HereMobileApp/hereapp-frontend
   ```
2. **Instalar dependencias**
   ```bash
   flutter pub get
   ```
3. **Abrir en Xcode**
   ```bash
   open ios/Runner.xcworkspace
   ```
4. **Configuración básica en Xcode**
- Seleccionar cualquier Team de desarrollo (puede ser personal)
- Cambiar Bundle Identifier a uno único: com.tuorganizacion.hereapp
- No es necesario configurar NFC capabilities