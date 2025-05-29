import 'dart:convert';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter_nfc_kit/flutter_nfc_kit.dart';
import 'package:http/http.dart' as http;

class NfcView extends StatefulWidget {
  final String? userEmail;
  final String? userRole; // Añadimos el rol del usuario

  const NfcView({Key? key, this.userEmail, this.userRole}) : super(key: key);

  @override
  State<NfcView> createState() => _NfcViewState();
}

class _NfcViewState extends State<NfcView> {
  bool _isReading = false;
  bool _isRegistering = false;
  bool _nfcSupported = true;
  bool _isLoading = true;
  String _userName = '';
  String _userSurname = '';
  bool _isAdmin = false; // Para determinar si es administrador
  String? _lastReadTag; // Para almacenar el último tag leído
  OverlayEntry? _currentNotification; // Para controlar notificaciones

  // URL del backend
  final String _baseUrl = 'https://heremobileapp-backend-1-0-0.onrender.com';

  @override
  void initState() {
    super.initState();
    _checkNfcSupport();
    _loadUserData();
    // Verificar si el usuario es administrador
    _checkIfAdmin();
  }

  @override
  void dispose() {
    // Limpiar notificación activa si existe
    _currentNotification?.remove();
    super.dispose();
  }

  // Verificar si el usuario es administrador basado en el rol recibido
  void _checkIfAdmin() {
    setState(() {
      _isAdmin = widget.userRole?.contains('ADMIN') ?? false;
    });
  }

  /// Llama a tu backend para obtener { nombre, apellido1 } a partir del email
  Future<void> _loadUserData() async {
    if (widget.userEmail == null) return;
    try {
      final uri = Uri.parse(
        '$_baseUrl/usuario?email=${Uri.encodeComponent(widget.userEmail!)}',
      );
      final resp = await http.get(
        uri,
        headers: {
          'Content-Type': 'application/json; charset=utf-8',
          'Accept': 'application/json; charset=utf-8',
        },
      );
      if (resp.statusCode == 200) {
        // Decodificar correctamente el JSON con UTF-8
        final String responseBody = utf8.decode(resp.bodyBytes);
        final Map<String, dynamic> data = jsonDecode(responseBody);
        setState(() {
          _userName = data['nombre'] ?? '';
          _userSurname = data['apellido1'] ?? '';
          _isLoading = false;
        });
      } else {
        debugPrint('Error al cargar usuario: ${resp.statusCode}');
        setState(() {
          _isLoading = false;
        });
      }
    } catch (e) {
      debugPrint('Exception _loadUserData: $e');
      setState(() {
        _isLoading = false;
      });
    }
  }

  // Método para verificar si el dispositivo soporta NFC
  Future<void> _checkNfcSupport() async {
    try {
      NFCAvailability availability = await FlutterNfcKit.nfcAvailability;
      setState(() {
        _nfcSupported = availability == NFCAvailability.available;
      });
    } catch (e) {
      setState(() {
        _nfcSupported = false;
      });
    }
  }

  // Mostrar diálogo de NFC estilo iOS
  void _showNFCDialog({required String title, required String message, required VoidCallback onCancel}) {
    showCupertinoDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => CupertinoAlertDialog(
        title: Text(
          title,
          style: const TextStyle(
            fontFamily: '.SF Pro Text',
            fontSize: 17,
            fontWeight: FontWeight.w600,
          ),
        ),
        content: Column(
          children: [
            const SizedBox(height: 16),
            Text(
              message,
              style: const TextStyle(
                fontFamily: '.SF Pro Text',
                fontSize: 13,
                color: Color(0xFF8E8E93),
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 20),
            // Icono NFC animado
            Container(
              width: 80,
              height: 80,
              decoration: BoxDecoration(
                color: const Color(0xFF007AFF).withOpacity(0.1),
                shape: BoxShape.circle,
              ),
              child: const Icon(
                CupertinoIcons.radiowaves_right,
                size: 40,
                color: Color(0xFF007AFF),
              ),
            ),
          ],
        ),
        actions: [
          CupertinoDialogAction(
            child: const Text(
              'Cancelar',
              style: TextStyle(
                fontFamily: '.SF Pro Text',
                color: Color(0xFF007AFF),
              ),
            ),
            onPressed: () {
              Navigator.of(context).pop();
              onCancel();
            },
          ),
        ],
      ),
    );
  }

  // Método para leer una tarjeta NFC y procesar asistencia
  Future<void> _readTag() async {
    if (Platform.isIOS) {
      // En iOS simulamos la lectura NFC
      _showNFCDialog(
        title: 'Listo para escanear',
        message: 'Acerca tu Tarjeta Transporte a la parte superior del móvil para realizar la lectura',
        onCancel: () {
          setState(() {
            _isReading = false;
          });
        },
      );

      // Simular lectura después de 2 segundos
      await Future.delayed(const Duration(seconds: 2));
      Navigator.of(context).pop(); // Cerrar diálogo

      setState(() {
        _lastReadTag = "ABCD1234";
        _isReading = false;
      });

      // Procesar asistencia con la tarjeta leída
      await _procesarAsistencia(_lastReadTag!);
      return;
    } else if (!_nfcSupported) {
      _showNotification(
          'NFC no disponible en este dispositivo',
          isSuccess: false
      );
      return;
    }

    setState(() {
      _isReading = true;
    });

    // Mostrar diálogo de NFC para Android también
    _showNFCDialog(
      title: 'Listo para escanear',
      message: 'Acerca la tarjeta al teléfono, de manera que estén en contactos.',
      onCancel: () {
        setState(() {
          _isReading = false;
        });
      },
    );

    try {
      // Verificar disponibilidad de NFC
      NFCAvailability availability = await FlutterNfcKit.nfcAvailability;
      if (availability != NFCAvailability.available) {
        Navigator.of(context).pop(); // Cerrar diálogo
        setState(() {
          _isReading = false;
        });
        _showNotification(
            'NFC no disponible en este dispositivo',
            isSuccess: false
        );
        return;
      }

      // Configurar mensajes según plataforma
      String alertMessage = Platform.isIOS
          ? "Mantenga la tarjeta cerca del iPhone para leerla."
          : "Acérquese a la tarjeta NFC...";

      // Inicia la sesión NFC (timeout 20s)
      final NFCTag tag = await FlutterNfcKit.poll(
        timeout: const Duration(seconds: 20),
        iosMultipleTagMessage: "Se detectaron múltiples tarjetas. Por favor, acerque solo una.",
        iosAlertMessage: alertMessage,
      );

      Navigator.of(context).pop(); // Cerrar diálogo

      // Guardar el UID para uso posterior
      setState(() {
        _lastReadTag = tag.id;
      });

      // Procesar asistencia con la tarjeta leída
      if (tag.id != null && tag.id!.isNotEmpty) {
        await _procesarAsistencia(tag.id!);
      } else {
        _showNotification(
            'Error: No se pudo obtener el UID de la tarjeta',
            isSuccess: false
        );
      }

    } catch (e) {
      Navigator.of(context).pop(); // Cerrar diálogo
      _showNotification(
          'Error al leer NFC: ${e.toString().substring(0, min(50, e.toString().length))}...',
          isSuccess: false
      );
    } finally {
      // Asegura cerrar la sesión
      try {
        await FlutterNfcKit.finish();
      } catch (_) {}

      setState(() {
        _isReading = false;
      });
    }
  }


  // Método principal para procesar la asistencia con el UID leído
  Future<void> _procesarAsistencia(String uidMifare) async {
    if (widget.userEmail == null) {
      _showNotification(
          'Error: No se encontró el email del usuario',
          isSuccess: false
      );
      return;
    }

    try {
      final uri = Uri.parse('$_baseUrl/asistencia/procesar-nfc');
      final resp = await http.post(
        uri,
        headers: {
          'Content-Type': 'application/json; charset=utf-8',
        },
        body: jsonEncode({
          'uidMifare': uidMifare,
          'emailUsuario': widget.userEmail!
        }),
      );

      // Decodificar respuesta con UTF-8 para todos los casos
      final String responseBody = utf8.decode(resp.bodyBytes);

      // Debug: mostrar código de estado y respuesta
      print('Status Code: ${resp.statusCode}');
      print('Response Body: $responseBody');

      // Lista más completa de mensajes de éxito
      bool isSuccessResponse = responseBody.contains('sesión creada') ||
          responseBody.contains('Sesión cerrada') ||
          responseBody.contains('Entrada registrada') ||
          responseBody.contains('Salida registrada') ||
          responseBody.contains('Nueva entrada registrada') ||
          responseBody.contains('Nueva sesión creada') ||
          responseBody.contains('cerrada tras') ||
          responseBody.contains('registrada correctamente');

      // Verificar si es un error de SQL pero la operación fue exitosa
      bool isSuccessfulOperation = responseBody.contains('Nueva sesión creada con ID:') ||
          responseBody.contains('Sesión cerrada tras') ||
          responseBody.contains('registrada correctamente');

      if (resp.statusCode == 200 || isSuccessResponse || isSuccessfulOperation) {
        // Asistencia procesada correctamente
        String displayMessage = responseBody;

        // Si hay error SQL pero operación exitosa, mostrar solo la parte exitosa
        if (resp.statusCode == 500 && isSuccessfulOperation) {
          if (responseBody.contains('Nueva sesión creada con ID:')) {
            displayMessage = "Nueva sesión creada correctamente";
          } else if (responseBody.contains('Sesión cerrada tras')) {
            displayMessage = responseBody.split('tras')[0] + 'tras' + responseBody.split('tras')[1].split(' minutos')[0] + ' minutos';
          } else if (responseBody.contains('registrada correctamente')) {
            displayMessage = "Entrada registrada correctamente";
          }
        }

        _showNotification(
            displayMessage,
            isSuccess: true
        );
      } else if (resp.statusCode == 404 || responseBody.contains('no está registrada')) {
        // Tarjeta no registrada
        _showNotification(
            responseBody,
            isSuccess: false,
            isWarning: true
        );
      } else if (resp.statusCode == 400) {
        // Error de validación (UID vacío, email vacío, etc.)
        _showNotification(
            responseBody,
            isSuccess: false
        );
      } else {
        // Solo mostrar error si realmente es un error y no contiene mensajes de éxito
        if (!isSuccessResponse && !isSuccessfulOperation) {
          _showNotification(
              'Error inesperado (${resp.statusCode}): $responseBody',
              isSuccess: false
          );
        } else {
          // Si contiene mensaje de éxito pero código de error, mostrar como éxito
          _showNotification(
              responseBody,
              isSuccess: true
          );
        }
      }
    } catch (e) {
      _showNotification(
          'Error de conexión: ${e.toString()}',
          isSuccess: false
      );
    }
  }

  // Método para registrar una tarjeta en la base de datos (solo administradores)
  Future<void> _registrarTarjeta() async {
    setState(() {
      _isRegistering = true;
    });

    // Mostrar diálogo de NFC para registrar
    _showNFCDialog(
      title: 'Registrar nueva tarjeta',
      message: 'Acerca la nueva tarjeta al teléfono para registrarla en el sistema.',
      onCancel: () {
        setState(() {
          _isRegistering = false;
        });
      },
    );

    try {
      if (Platform.isIOS) {
        // Simular lectura en iOS
        await Future.delayed(const Duration(seconds: 2));
        Navigator.of(context).pop(); // Cerrar diálogo

        setState(() {
          _lastReadTag = "NEWCARD123";
        });

        await _enviarRegistroAlBackend(_lastReadTag!);
      } else {
        // Proceso real de NFC en Android
        NFCAvailability availability = await FlutterNfcKit.nfcAvailability;
        if (availability != NFCAvailability.available) {
          Navigator.of(context).pop();
          _showNotification('NFC no disponible en este dispositivo', isSuccess: false);
          return;
        }

        final NFCTag tag = await FlutterNfcKit.poll(
          timeout: const Duration(seconds: 20),
          iosAlertMessage: "Acerque la tarjeta para registrarla",
        );

        Navigator.of(context).pop(); // Cerrar diálogo

        if (tag.id != null && tag.id!.isNotEmpty) {
          setState(() {
            _lastReadTag = tag.id;
          });
          await _enviarRegistroAlBackend(tag.id!);
        } else {
          _showNotification('No se pudo leer la tarjeta para registrarla', isSuccess: false);
        }
      }
    } catch (e) {
      Navigator.of(context).pop(); // Cerrar diálogo si está abierto
      _showNotification(
          'Error al leer tarjeta para registro: ${e.toString().substring(0, min(50, e.toString().length))}...',
          isSuccess: false
      );
    } finally {
      try {
        await FlutterNfcKit.finish();
      } catch (_) {}

      setState(() {
        _isRegistering = false;
      });
    }
  }

  // Método para enviar el registro al backend (solo para administradores)
  // Método para enviar el registro al backend (solo para administradores)
  Future<void> _enviarRegistroAlBackend(String uidMifare) async {
    try {
      final uri = Uri.parse('$_baseUrl/usuario/registrar-mifare');
      final resp = await http.post(
        uri,
        headers: {'Content-Type': 'application/json; charset=utf-8'},
        body: jsonEncode({
          'uidMifare': uidMifare,
          'emailUsuario': widget.userEmail // Incluir el email del usuario
        }),
      );

      // Decodificar respuesta con UTF-8
      final String responseBody = utf8.decode(resp.bodyBytes);

      if (resp.statusCode == 200) {
        _showNotification(
            'Tarjeta registrada correctamente',
            isSuccess: true
        );
      } else if (resp.statusCode == 409) {
        // Código 409 = Conflict, significa que la tarjeta ya está registrada
        _showNotification(
            'Esta tarjeta ya está registrada en el sistema',
            isSuccess: false,
            isWarning: true
        );
      } else if (resp.statusCode == 403) {
        // Código 403 = Forbidden, sin permisos de administrador
        _showNotification(
            'No tiene permisos para registrar tarjetas',
            isSuccess: false
        );
      } else if (resp.statusCode == 400) {
        // Código 400 = Bad Request, UID vacío o inválido
        _showNotification(
            'UID de tarjeta inválido',
            isSuccess: false
        );
      } else {
        _showNotification(
            'Error al registrar tarjeta: $responseBody',
            isSuccess: false
        );
      }
    } catch (e) {
      _showNotification(
          'Error de conexión al registrar: ${e.toString()}',
          isSuccess: false
      );
    }
  }

  // Método para mostrar notificaciones en estilo iOS sin solapamiento
  void _showNotification(String message, {bool isSuccess = true, bool isWarning = false}) {
    // Remover notificación anterior si existe
    _currentNotification?.remove();

    Color backgroundColor;
    IconData iconData;

    if (isWarning) {
      backgroundColor = const Color(0xFFDF3939); // Naranja para warnings
      iconData = CupertinoIcons.exclamationmark_triangle_fill;
    } else if (isSuccess) {
      backgroundColor = const Color(0xFF34C759); // Verde para éxito
      iconData = CupertinoIcons.checkmark_circle_fill;
    } else {
      backgroundColor = const Color(0xFFFF3B30); // Rojo para errores
      iconData = CupertinoIcons.xmark_circle_fill;
    }

    _showIOSStyleToast(message, backgroundColor, iconData);
  }

  // Método auxiliar para obtener el mínimo entre dos enteros
  int min(int a, int b) {
    return a < b ? a : b;
  }

  // Mostrar un toast estilo iOS mejorado con icono específico
  void _showIOSStyleToast(String message, Color backgroundColor, IconData iconData) {
    final overlay = Overlay.of(context);

    // Calcular posición dinámica para evitar solapar botones
    double bottomPosition = _isAdmin ? 40.0 : 100.0;

    // Crear nueva notificación
    final newNotification = OverlayEntry(
      builder: (context) => Positioned(
        bottom: bottomPosition,
        left: 16,
        right: 16,
        child: Center(
          child: Material(
            color: Colors.transparent,
            child: Container(
              constraints: const BoxConstraints(maxWidth: 350),
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
              decoration: BoxDecoration(
                color: backgroundColor,
                borderRadius: BorderRadius.circular(20),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.15),
                    blurRadius: 10,
                    offset: const Offset(0, 4),
                  ),
                ],
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(
                    iconData,
                    color: Colors.white,
                    size: 20,
                  ),
                  const SizedBox(width: 12),
                  Flexible(
                    child: Text(
                      message,
                      style: const TextStyle(
                        fontFamily: '.SF Pro Text',
                        color: Colors.white,
                        fontSize: 15,
                        fontWeight: FontWeight.w500,
                      ),
                      textAlign: TextAlign.center,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );

    // Reemplazar la notificación actual
    _currentNotification = newNotification;
    overlay.insert(_currentNotification!);

    // Remover después de 4 segundos (un poco más tiempo para leer mensajes largos)
    Future.delayed(const Duration(seconds: 4), () {
      if (_currentNotification == newNotification) {
        _currentNotification?.remove();
        _currentNotification = null;
      }
    });
  }

  void _cerrarSesion() {
    showCupertinoDialog(
      context: context,
      builder: (context) => CupertinoAlertDialog(
        title: const Text('Cerrar sesión'),
        content: const Text('¿Estás seguro de que deseas cerrar la sesión?'),
        actions: [
          CupertinoDialogAction(
            isDestructiveAction: true,
            child: const Text('Cancelar'),
            onPressed: () => Navigator.of(context).pop(),
          ),
          CupertinoDialogAction(
            child: const Text('Cerrar sesión'),
            onPressed: () {
              Navigator.of(context).pop();
              Navigator.of(context).pushReplacementNamed('/');
            },
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    // Usamos el mismo scaffold con estilo iOS para ambas plataformas
    return _buildIOSStyleScaffold();
  }

  // Construir Scaffold con estilo iOS para ambas plataformas
  Widget _buildIOSStyleScaffold() {
    return Theme(
      // Tema global para usar la tipografía de iOS en toda la app
      data: ThemeData(
        fontFamily: '.SF Pro Text', // Tipografía de iOS
        textTheme: const TextTheme(
          // Define todos los estilos de texto para usar la tipografía de iOS
          bodyLarge: TextStyle(fontFamily: '.SF Pro Text'),
          bodyMedium: TextStyle(fontFamily: '.SF Pro Text'),
          bodySmall: TextStyle(fontFamily: '.SF Pro Text'),
          titleLarge: TextStyle(fontFamily: '.SF Pro Text'),
          titleMedium: TextStyle(fontFamily: '.SF Pro Text'),
          titleSmall: TextStyle(fontFamily: '.SF Pro Text'),
          labelLarge: TextStyle(fontFamily: '.SF Pro Text'),
          labelMedium: TextStyle(fontFamily: '.SF Pro Text'),
          labelSmall: TextStyle(fontFamily: '.SF Pro Text'),
        ),
      ),
      child: Material(
        // Usamos Material como base pero con estética iOS
        color: Colors.white,
        child: _isLoading
            ? const Center(child: CupertinoActivityIndicator(radius: 16))
            : Stack(
          children: [
            // Contenido principal con SafeArea
            SafeArea(
              child: Column(
                children: [
                  // Header con logo alineado horizontalmente con el botón de cerrar sesión
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.only(
                        top: 10,
                        bottom: 10,
                        left: 16,
                        right: 16
                    ),
                    color: Colors.white,
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        // Logo a la izquierda alineado con botón de cerrar sesión
                        SizedBox(
                          height: 50, // Reducido para alinearse con el botón
                          child: Image.asset(
                            'assets/images/logotipo_here.png',
                            height: 100,
                            fit: BoxFit.fitHeight,
                          ),
                        ),
                        // Botón cerrar sesión
                        CupertinoButton(
                          padding: const EdgeInsets.all(5),
                          child: const Icon(
                            CupertinoIcons.arrow_uturn_right,
                            color: Color(0xFF007AFF), // Azul iOS
                            size: 24,
                          ),
                          onPressed: _cerrarSesion,
                        ),
                      ],
                    ),
                  ),
                  // Resto del contenido
                  Expanded(child: _buildIOSStyleBody()),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  // Construir cuerpo con estilo iOS
  Widget _buildIOSStyleBody() {
    // Estilos unificados para ambas plataformas con tipografía iOS
    const TextStyle normalTextStyle = TextStyle(
      fontFamily: '.SF Pro Text', // Tipografía iOS
      fontSize: 24,
      fontWeight: FontWeight.bold,
      color: Color(0xFF007AFF), // Color azul iOS
      decoration: TextDecoration.none,
    );

    const TextStyle errorTextStyle = TextStyle(
      fontFamily: '.SF Pro Text', // Tipografía iOS
      color: Color(0xFF8E8E93), // Gris iOS
      fontSize: 15,
      fontWeight: FontWeight.normal,
      decoration: TextDecoration.none,
    );

    return Padding(
      padding: const EdgeInsets.all(24.0),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          // Mensaje de bienvenida con nombre y apellido
          Text(
            'Bienvenido $_userName $_userSurname',
            style: normalTextStyle,
            textAlign: TextAlign.center,
          ),

          // Mostrar el rol si es administrador
          if (_isAdmin) ...[
            const SizedBox(height: 10),
            Text(
              'Vista de administrador',
              style: const TextStyle(
                fontFamily: '.SF Pro Text',
                fontSize: 16,
                fontWeight: FontWeight.w500,
                color: Color(0xFF8E8E93), // Gris iOS
              ),
              textAlign: TextAlign.center,
            ),
          ],

          const SizedBox(height: 50),

          // Imagen de NFC con estilo iOS para ambas plataformas
          Icon(
            CupertinoIcons.radiowaves_right,
            size: 100,
            color: _nfcSupported
                ? const Color(0xFF007AFF) // Color azul iOS
                : const Color(0xFFC7C7CC), // Gris claro iOS
          ),

          const SizedBox(height: 50),

          // Botón grande para leer NFC con estilo iOS
          SizedBox(
            width: double.infinity,
            height: 50,
            child: CupertinoButton(
              padding: EdgeInsets.zero,
              color: const Color(0xFF007AFF), // Color azul iOS
              borderRadius: BorderRadius.circular(12),
              onPressed: (_isReading || _isRegistering) ? null : _readTag,
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(
                    CupertinoIcons.radiowaves_right,
                    color: Colors.white,
                  ),
                  const SizedBox(width: 8),
                  Text(
                    'Leer tarjeta NFC',
                    style: const TextStyle(
                      fontFamily: '.SF Pro Text', // Tipografía iOS
                      fontSize: 18,
                      color: Colors.white,
                      fontWeight: FontWeight.w500, // Semibold
                    ),
                  ),
                ],
              ),
            ),
          ),

          // Botón para registrar tarjetas si es administrador
          if (_isAdmin) ...[
            const SizedBox(height: 20),
            SizedBox(
              width: double.infinity,
              height: 50,
              child: CupertinoButton(
                padding: EdgeInsets.zero,
                color: const Color(0xFF34C759), // Verde iOS
                borderRadius: BorderRadius.circular(12),
                onPressed: (_isReading || _isRegistering) ? null : _registrarTarjeta,
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const Icon(
                      CupertinoIcons.plus_circle,
                      color: Colors.white,
                    ),
                    const SizedBox(width: 8),
                    Text(
                      'Registrar nueva tarjeta',
                      style: const TextStyle(
                        fontFamily: '.SF Pro Text', // Tipografía iOS
                        fontSize: 18,
                        color: Colors.white,
                        fontWeight: FontWeight.w500, // Semibold
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],

          // Mensaje si NFC no está soportado
          if (!_nfcSupported) ...[
            const SizedBox(height: 20),
            Text(
              'Este dispositivo no soporta NFC o NFC está desactivado',
              style: errorTextStyle,
              textAlign: TextAlign.center,
            ),
          ],
        ],
      ),
    );
  }
}