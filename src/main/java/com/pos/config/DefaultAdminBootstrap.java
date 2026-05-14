package com.pos.config;


@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        name = "app.bootstrap.default-admin.enabled",
        havingValue = "true"
)
public class DefaultAdminBootstrap implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.default-admin.username:admin1}")
    private String adminUsername;

    @Value("${app.bootstrap.default-admin.password:admin1}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarioRepository.existsByRol_Nombre("ADMIN")) {
            return;
        }

        Rol rolAdmin = rolRepository.findByNombre("ADMIN")
                .orElseGet(() -> rolRepository.save(Rol.builder().nombre("ADMIN").build()));

        Usuario usuarioExistente = usuarioRepository.findByUsername(adminUsername).orElse(null);
        if (usuarioExistente != null) {
            usuarioExistente.setRol(rolAdmin);
            usuarioExistente.setPassword(passwordEncoder.encode(adminPassword));
            usuarioExistente.setActivo(true);
            usuarioRepository.save(usuarioExistente);
            log.info("Usuario existente promovido a ADMIN: {}", adminUsername);
            return;
        }

        Usuario admin = Usuario.builder()
                .username(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .rol(rolAdmin)
                .activo(true)
                .build();

        usuarioRepository.save(admin);
        log.info("Usuario administrador por defecto creado: {}", adminUsername);
    }
}

