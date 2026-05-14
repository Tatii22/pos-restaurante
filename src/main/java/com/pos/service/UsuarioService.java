package com.pos.service;


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

    public List<UsuarioResponseDTO> listarUsuarios() {
        return usuarioRepository.findAll()
                .stream()
                .map(u -> new UsuarioResponseDTO(
                        u.getId(),
                        u.getUsername(),
                        u.getRol() != null && u.getRol().getNombre() != null ? u.getRol().getNombre() : "SIN_ROL",
                        Boolean.TRUE.equals(u.getActivo())
                ))
                .toList();
    }

    public UsuarioResponseDTO actualizarUsuario(Long id, UsuarioUpdateDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));

        String nuevoUsername = dto.getUsername().trim();
        usuarioRepository.findByUsername(nuevoUsername)
                .filter(existente -> !existente.getId().equals(id))
                .ifPresent(existente -> {
                    throw new BadRequestException("El usuario ya existe");
                });

        Rol rol = rolRepository.findByNombre(dto.getRol())
                .orElseThrow(() -> new BadRequestException("Rol inválido"));
        boolean usuarioActualEsAdmin = usuario.getRol() != null && "ADMIN".equals(usuario.getRol().getNombre());
        if ("ADMIN".equals(rol.getNombre()) && !usuarioActualEsAdmin) {
            throw new BadRequestException("No se puede asignar rol ADMIN");
        }
        if (usuarioActualEsAdmin && !"ADMIN".equals(rol.getNombre())) {
            throw new BadRequestException("No se puede cambiar el rol de un usuario ADMIN");
        }

        usuario.setUsername(nuevoUsername);
        usuario.setRol(rol);
        usuario.setActivo(dto.getActivo() == null || dto.getActivo());

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        usuarioRepository.save(usuario);

        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getRol().getNombre(),
                Boolean.TRUE.equals(usuario.getActivo())
        );
    }

    public void eliminarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));
        if (usuario.getRol() != null && "ADMIN".equals(usuario.getRol().getNombre())) {
            throw new BadRequestException("No se puede eliminar un usuario ADMIN");
        }
        usuarioRepository.delete(usuario);
    }
}

