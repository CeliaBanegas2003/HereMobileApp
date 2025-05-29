import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/cupertino.dart';
import 'package:http/http.dart' as http;
import 'nfcView.dart';

void main() {
  runApp(const MyApp());
}

/// Aplicación principal con estilo iOS unificado
class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'HereApp',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        // Usar tipografía de iOS en toda la aplicación
        fontFamily: '.SF Pro Text',
        primaryColor: const Color(0xFF007AFF), // Azul iOS estándar
        colorScheme: ColorScheme.light(
          primary: const Color(0xFF007AFF), // Azul iOS
          secondary: const Color(0xFF34C759), // Verde iOS
          onPrimary: Colors.white,
          onSecondary: Colors.white,
        ),
        textTheme: const TextTheme(
          // Aplicar tipografía iOS a todos los estilos de texto
          displayLarge: TextStyle(fontFamily: '.SF Pro Text'),
          displayMedium: TextStyle(fontFamily: '.SF Pro Text'),
          displaySmall: TextStyle(fontFamily: '.SF Pro Text'),
          headlineLarge: TextStyle(fontFamily: '.SF Pro Text'),
          headlineMedium: TextStyle(fontFamily: '.SF Pro Text'),
          headlineSmall: TextStyle(fontFamily: '.SF Pro Text'),
          titleLarge: TextStyle(fontFamily: '.SF Pro Text'),
          titleMedium: TextStyle(fontFamily: '.SF Pro Text'),
          titleSmall: TextStyle(fontFamily: '.SF Pro Text'),
          bodyLarge: TextStyle(fontFamily: '.SF Pro Text'),
          bodyMedium: TextStyle(fontFamily: '.SF Pro Text'),
          bodySmall: TextStyle(fontFamily: '.SF Pro Text'),
          labelLarge: TextStyle(fontFamily: '.SF Pro Text'),
          labelMedium: TextStyle(fontFamily: '.SF Pro Text'),
          labelSmall: TextStyle(fontFamily: '.SF Pro Text'),
        ),
        // Estilo iOS para botones
        elevatedButtonTheme: ElevatedButtonThemeData(
          style: ElevatedButton.styleFrom(
            backgroundColor: const Color(0xFF007AFF),
            foregroundColor: Colors.white,
            textStyle: const TextStyle(
              fontFamily: '.SF Pro Text',
              fontWeight: FontWeight.w500,
            ),
            padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 16),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
          ),
        ),
        // Estilo iOS para campos de texto
        inputDecorationTheme: InputDecorationTheme(
          filled: true,
          fillColor: const Color(0xFFF2F2F7),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(10),
            borderSide: BorderSide.none,
          ),
          contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
          labelStyle: const TextStyle(
            fontFamily: '.SF Pro Text',
            fontWeight: FontWeight.w400,
            fontSize: 16,
          ),
        ),
      ),
      // Definir las rutas de la aplicación
      routes: {
        '/': (context) => const LoginScreen(),
      },
      initialRoute: '/',
    );
  }
}

/// Pantalla de Login con layout responsivo y estilo iOS
class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  // Controladores para correo y contraseña
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();

  bool _isPasswordObscured = true;
  bool _isLoading = false;
  bool _isWhiteExpanded = false; // Controla la animación del fondo blanco
  String _message = '';

  // URL del backend
  final String _backendUrl = 'https://heremobileapp-backend-1-0-0.onrender.com';

  Future<void> _login() async {
    final String email = _emailController.text.trim();
    final String password = _passwordController.text;

    if (email.isEmpty || password.isEmpty) {
      setState(() {
        _message = 'Por favor, completa todos los campos.';
      });
      return;
    }

    setState(() {
      _isLoading = true;
      _message = '';
    });

    try {
      // Primer paso: autenticar al usuario
      final loginUrl = Uri.parse('$_backendUrl/login');
      final loginResponse = await http.post(
        loginUrl,
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          "email": email,
          "contrasena": password,
        }),
      );

      if (loginResponse.statusCode == 200) {
        // El backend devuelve una cadena con el rol o "NONE"
        final String role = loginResponse.body;

        if (role == "NONE") {
          setState(() {
            _message = 'Credenciales incorrectas';
            _isLoading = false;
          });
          return;
        }

        // Navega a NfcView pasando el email y el rol
        Navigator.pushReplacement(
          context,
          CupertinoPageRoute(
            builder: (context) => NfcView(
              userEmail: email,
              userRole: role,
            ),
          ),
        );
      } else {
        setState(() {
          _message = 'Credenciales incorrectas o error: ${loginResponse.body}';
          _isLoading = false;
        });
      }
    } catch (e) {
      setState(() {
        _message = 'Error de conexión: $e';
        _isLoading = false;
      });
    }
  }

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    // Construir una interfaz con estética iOS para todas las plataformas
    final double screenWidth = MediaQuery.of(context).size.width;
    final double screenHeight = MediaQuery.of(context).size.height;
    double availableWidth = screenWidth - 80;
    if (availableWidth < 0) availableWidth = screenWidth;
    double cardWidth = availableWidth * 0.8;
    if (cardWidth > 350) cardWidth = 350;
    double contentHeight = 100 + 40 + 20 + 60 + 10 + 50;
    if (_message.isNotEmpty) contentHeight += 0;
    double cardHeight = contentHeight + 40;

    return Scaffold(
      resizeToAvoidBottomInset: false,
      body: Stack(
        children: [
          // Fondo completo (imagen con overlay oscuro)
          Positioned.fill(
            child: Image.asset(
              'assets/images/ufvFondo.png',
              fit: BoxFit.cover,
            ),
          ),
          Positioned.fill(
            child: Container(
              color: Colors.black.withOpacity(0.55),
            ),
          ),
          // Panel blanco animado con sombreado que se expande al pulsar el campo de correo
          Align(
            alignment: Alignment.center,
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 500),
              curve: Curves.easeOut,
              width: _isWhiteExpanded ? screenWidth : cardWidth,
              height: _isWhiteExpanded ? screenHeight : cardHeight,
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(_isWhiteExpanded ? 0 : 10),
                boxShadow: [
                  BoxShadow(
                    blurRadius: 10,
                    offset: const Offset(0, 4),
                  ),
                ],
              ),
            ),
          ),
          // Contenido del formulario de login centrado
          Center(
            child: SingleChildScrollView(
              padding: const EdgeInsets.symmetric(horizontal: 40),
              child: FractionallySizedBox(
                widthFactor: 0.9,
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 350),
                  child: AnimatedPhysicalModel(
                    duration: const Duration(milliseconds: 500),
                    curve: Curves.easeOut,
                    elevation: _isWhiteExpanded ? 0 : 10,
                    color: Colors.white,
                    shadowColor: Colors.black,
                    shape: BoxShape.rectangle,
                    borderRadius:
                    BorderRadius.circular(_isWhiteExpanded ? 0 : 10),
                    child: Padding(
                      padding: const EdgeInsets.all(20),
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          // Logotipo de la UFV
                          Image.asset(
                            'assets/images/UFVLogo.png',
                            height: 55,
                          ),
                          const SizedBox(height: 15),

                          // Usar CupertinoTextField para una experiencia iOS consistente en todas las plataformas
                          CupertinoTextField(
                            controller: _emailController,
                            keyboardType: TextInputType.emailAddress,
                            placeholder: 'Correo',
                            padding: const EdgeInsets.all(14),
                            decoration: BoxDecoration(
                              color: const Color(0xFFF2F2F7),
                              borderRadius: BorderRadius.circular(10),
                            ),
                            style: const TextStyle(
                              fontFamily: '.SF Pro Text',
                              fontSize: 16,
                              color: Colors.black87,
                            ),
                            onTap: () {
                              setState(() {
                                _isWhiteExpanded = true;
                              });
                            },
                          ),

                          const SizedBox(height: 15),

                          // Contraseña con estilo iOS
                          CupertinoTextField(
                            controller: _passwordController,
                            keyboardType: TextInputType.text,
                            placeholder: 'Contraseña',
                            padding: const EdgeInsets.all(14),
                            obscureText: _isPasswordObscured,
                            decoration: BoxDecoration(
                              color: const Color(0xFFF2F2F7),
                              borderRadius: BorderRadius.circular(10),
                            ),
                            style: const TextStyle(
                              fontFamily: '.SF Pro Text',
                              fontSize: 16,
                              color: Colors.black87,
                            ),
                            suffix: GestureDetector(
                              onTap: () {
                                setState(() {
                                  _isPasswordObscured = !_isPasswordObscured;
                                });
                              },
                              child: Padding(
                                padding: const EdgeInsets.only(right: 10),
                                child: Icon(
                                  _isPasswordObscured
                                      ? CupertinoIcons.eye
                                      : CupertinoIcons.eye_slash,
                                  color: Colors.grey,
                                  size: 20,
                                ),
                              ),
                            ),
                          ),

                          const SizedBox(height: 20),

                          // Botón de inicio de sesión con estilo iOS
                          _isLoading
                              ? const CupertinoActivityIndicator(radius: 14)
                              : SizedBox(
                            width: double.infinity,
                            height: 50,
                            child: CupertinoButton(
                              padding: EdgeInsets.zero,
                              color: const Color(0xFF007AFF),
                              borderRadius: BorderRadius.circular(12),
                              onPressed: _login,
                              child: const Text(
                                'Iniciar sesión',
                                style: TextStyle(
                                  fontFamily: '.SF Pro Text',
                                  fontSize: 17,
                                  color: Colors.white,
                                ),
                              ),
                            ),
                          ),

                          const SizedBox(height: 10),

                          // Mensaje de error con estilo iOS
                          Text(
                            _message,
                            style: const TextStyle(
                                fontFamily: '.SF Pro Text',
                                color: Color(0xFFFF3B30), // Rojo iOS
                                fontSize: 15,
                                fontWeight: FontWeight.w400), // Peso normal en iOS
                            textAlign: TextAlign.center,
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}