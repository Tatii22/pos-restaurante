package com.pos.service;
import com.pos.dto.usuario.UsuarioCreateDTO;
import com.pos.dto.usuario.UsuarioResponseDTO;
import com.pos.entity.Rol;
import com.pos.entity.Usuario;
import com.pos.exception.BadRequestException;
import com.pos.repository.RolRepository;
import com.pos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioResponseDTO crearUsuario(UsuarioCreateDTO dto) {

        if (usuarioRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new BadRequestException("El usuario ya existe");
        }

        Rol rol = rolRepository.findByNombre(dto.getRol())
                .orElseThrow(() -> new BadRequestException("Rol inválido"));

        if (rol.getNombre().equals("ADMIN")) {
            throw new BadRequestException("No se puede crear ADMIN");
        }

        Usuario usuario = Usuario.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .rol(rol)
                .activo(true)
                .build();

        usuarioRepository.save(usuario);

        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getUsername(),
                rol.getNombre(),
                usuario.getActivo()
        );
    }
    public Usuario obtenerPorUsername(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));
    }
}
