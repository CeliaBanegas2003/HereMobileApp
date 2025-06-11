# 📱 Here - App Móvil / Manual de Usuario

¡Bienvenido al repositorio de la aplicación móvil del **Sistema Optimizador de Tiempo de Asistencia a Clase**! Este documento te guiará a través de la instalación, requisitos y uso de la app según tu tipo de usuario.

---

## 📝 Contenido

* [Introducción](#introducción)
* [Requisitos del Sistema](#requisitos-del-sistema)

    * [Hardware](#hardware)
    * [Software](#software)
* [Tipos de Usuario](#tipos-de-usuario)
* [Acceso a la Aplicación](#acceso-a-la-aplicación)

    * [Primer Acceso](#primer-acceso)
    * [Inicio de Sesión](#inicio-de-sesión)
* [Funcionalidades por Tipo de Usuario](#funcionalidades-por-tipo-de-usuario)

    * [Alumno](#alumno)
    * [Profesor](#profesor)
    * [Administrador](#administrador)

---

## 📖 Introducción

La aplicación móvil del Sistema Optimizador de Tiempo de Asistencia a Clase permite el registro automatizado de asistencia mediante tecnología **NFC** y tarjetas **MIFARE**. A continuación encontrarás las instrucciones detalladas para sacar el máximo provecho de la app según tu perfil.

---

## 🖥 Requisitos del Sistema

### Hardware

* Dispositivo móvil Android **7.0** o superior
* **NFC** habilitado en el dispositivo
* Conexión a Internet (Wi‑Fi o datos móviles)

### Software

* Aplicación **Sistema Optimizador de Tiempo de Asistencia** instalada
* **NFC** activado en la configuración del dispositivo

---

## 👥 Tipos de Usuario

La aplicación reconoce automáticamente tres tipos de usuario según tus credenciales:

| Usuario           | Permisos principales            |
| ----------------- |---------------------------------|
| **Alumno**        | Registro de asistencia personal |
| **Profesor**      | Creación de sesiones            |
| **Administrador** | Gestión de tarjetas             |

---

## 🚀 Acceso a la Aplicación

### Primer Acceso

1. **Descarga e instalación**: Instala el APK facilitado por la institución.
2. **Configuración NFC**: Activa NFC en **Ajustes > Conexiones > NFC**.
3. **Credenciales**: Introduce las credenciales proporcionadas por la administración.

### Inicio de Sesión

```plaintext
1. Abre la aplicación
2. Ingresa tu correo institucional
3. Ingresa tu contraseña
4. Pulsa "Iniciar Sesión"
```



---

## 🔧 Funcionalidades por Tipo de Usuario

### Alumno

#### Registro de Asistencia

1. Abre la app e inicia sesión.
2. Selecciona **Leer Tarjeta NFC**.
3. Acerca tu móvil a la tarjeta MIFARE (2-6 cm).
4. Espera confirmación visual y auditiva.
5. Verifica el mensaje: **"Asistencia registrada correctamente"**.

> **Al finalizar la clase**, repite el proceso para registrar la salida.

**Indicadores Visuales:**

* 🟢 Verde: Éxito
* 🔴 Rojo: Error o tarjeta no válida
* 🟡 Amarillo: Procesando lectura

---

### Profesor

#### Gestión de Sesiones de Clase

1. Abre la app antes de la clase.
2. Pulsa **Leer tarjeta NFC**.
3. Acerca tu móvil a la tarjeta MIFARE (2-6 cm).
4. Espera confirmación visual y auditiva.
5. Verifica el mensaje: **"Sesión iniciada correctamente"**.
4. La sesión queda activa para recibir registros.


#### Finalización de Sesión

1. Vuelve a pulsar en **Leer tarjeta NFC**.

---

### Administrador

El **Administrador** combina las funciones de **Profesor** y **Alumno**, añadiendo:

#### Registro de Tarjetas MIFARE


1. Pulsa **Registrar Tarjeta MIFARE**. 
2. Acerca el móvil a la tarjeta.
3. Verifica el mensaje: **"Tarjeta registrada exitosamente"**.

---

## 🤝 Contribuciones

¡Tus sugerencias son bienvenidas! Para reportar errores o proponer mejoras, abre un issue o envía un pull request.

---

## 📄 Licencia

Distributed under the **MIT License**. Consulta el archivo `LICENSE` para más detalles.
