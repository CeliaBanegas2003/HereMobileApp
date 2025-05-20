package com.example.hereapp_backend.dataAccess;


import com.example.hereapp_backend.models.Rol;
import com.example.hereapp_backend.models.Usuario;
import com.example.hereapp_backend.repository.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LogInBBDD {

    @Autowired
    private UsuarioRepository usuarioRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();



    public String validarTipoUsuario(String email, String contrasena) {
        email = email.trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email);
        if (usuario == null) {
            return null;
        }
        // Validar la contraseña principal (asumiendo que está encriptada con BCrypt)
        if (passwordEncoder.matches(contrasena, usuario.getContrasena())) {
            // Si hay una contraseña provisional, la removemos (ya se usó)
            if (usuario.getContrasenaProvisional() != null) {
                usuario.setContrasenaProvisional(null);
                usuarioRepository.save(usuario);
            }
            // Construir una cadena que contenga los roles del usuario
            StringBuilder rolesBuilder = new StringBuilder();
            if (usuario.getRoles().contains(Rol.ADMIN)) {
                rolesBuilder.append("ADMIN,");
            }
            if (usuario.getRoles().contains(Rol.PROFESOR)) {
                rolesBuilder.append("PROFESOR,");
            }
            if (usuario.getRoles().contains(Rol.ALUMNO)) {
                rolesBuilder.append("ALUMNO,");
            }
            if (rolesBuilder.length() > 0) {
                // Eliminar la última coma
                rolesBuilder.setLength(rolesBuilder.length() - 1);
                return rolesBuilder.toString();
            }
            return "NONE";
        }
        // Validar la contraseña provisional, si existe
        if (usuario.getContrasenaProvisional() != null &&
                passwordEncoder.matches(contrasena, usuario.getContrasenaProvisional())) {
            return "PROVISIONAL";
        }
        return null;
    }

}
