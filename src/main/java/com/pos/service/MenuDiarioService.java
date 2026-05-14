package com.pos.service;



@Service
@RequiredArgsConstructor
public class MenuDiarioService {

    private final MenuDiarioRepository menuDiarioRepository;
    private final InventarioDiarioRepository inventarioDiarioRepository;
    private final FechaOperativaService fechaOperativaService;

    @Transactional
    public MenuDiario crearMenuHoy(Usuario usuario) {
        LocalDate hoy = fechaOperativaService.obtenerFechaOperativa();

        // Idempotente por dia: si ya existe registro (activo o inactivo), se reutiliza.
        MenuDiario menuExistente = menuDiarioRepository.findByFecha(hoy).orElse(null);
        if (menuExistente != null) {
            // Si se reutiliza el menu del dia, se limpia inventario previo para evitar
            // errores "Producto ya existe" al iniciar un nuevo turno.
            inventarioDiarioRepository.deleteByMenuDiario(menuExistente);
            menuExistente.setActivo(true);
            menuExistente.setUsuario(usuario);
            return menuDiarioRepository.save(menuExistente);
        }

        MenuDiario menu = MenuDiario.builder()
                .fecha(hoy)
                .usuario(usuario)
                .activo(true)
                .build();

        return menuDiarioRepository.save(menu);
    }

    public MenuDiario obtenerMenuActivo() {
        return menuDiarioRepository
                .findByFechaAndActivoTrue(fechaOperativaService.obtenerFechaOperativa())
                .orElseThrow(() -> new BadRequestException("No hay menu activo hoy"));
    }
}

