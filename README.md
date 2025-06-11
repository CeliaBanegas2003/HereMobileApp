# Here! - Aplicación Móvil / Sistema de Control de Asistencia NFC

> **Autor**: Celia Banegas Banegas

> **🎓 Proyecto Fin de Grado presentado en la Universidad Francisco de Vitoria**

Un sistema integral para la automatización del control de asistencia estudiantil mediante tecnología NFC, compuesto por una aplicación móvil Flutter y una aplicación web con backend Java Spring Boot.
## 📋 Descripción del Proyecto

HereMobileApp es un sistema completo que automatiza el registro de asistencia en entornos educativos utilizando tecnología NFC. El sistema elimina la dependencia de métodos manuales, reduciendo el tiempo dedicado al pase de lista de 5-6 minutos a menos de 45 segundos por sesión.

### Componentes Principales

- **Backend (Java Spring Boot)**: API REST para gestión de datos y lógica de negocio
- **Frontend Web (Vaadin)**: Plataforma web para gestión administrativa
- **Aplicación Móvil (Flutter)**: App para profesores con funcionalidad NFC
- **Sistema de Tarjetas MIFARE**: Identificación única por estudiante
## 🛠️ Tecnologías Utilizadas

### Backend (hereapp-backend)
- **Java 21**: Lenguaje principal
- **Spring Boot 3**: Framework principal
- **Spring Security**: Autenticación y autorización
- **Spring Data JPA**: Persistencia de datos
- **MySQL**: Base de datos relacional
- **Vaadin 24**: Framework para aplicación web

### Frontend Móvil (hereapp-frontend)
- **Flutter 3.24+**: Framework multiplataforma
- **Dart**: Lenguaje de programación
- **nfc_manager**: Gestión de comunicación NFC
- **http**: Comunicación con APIs REST
- **shared_preferences**: Persistencia local
- **flutter_secure_storage**: Almacenamiento seguro

### Infraestructura
- **Docker & Docker Compose**: Contenerización
- **GitHub Actions**: CI/CD
- **AWS RDS**: Base de datos en la nube
- **Render**: Hosting y despliegue

### 📱 [Manual de Instalación](./docs/manual-instalacion.md)
Guía completa para instalar y configurar la aplicación móvil en Android e iOS.

### 👤 [Manual de Usuario - Aplicación Móvil](./docs/manual-usuario-movil.md)
Instrucciones detalladas de uso para estudiantes, profesores y administradores.

## 📄 Licencia

Distributed under the **MIT License**. 

