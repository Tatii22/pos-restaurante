package com.pos.controller;







@RestController
@RequestMapping("/menu-diario")
@RequiredArgsConstructor
public class MenuDiarioController {

    private final MenuDiarioService menuDiarioService;
    private final UsuarioRepository usuarioRepository;

    @PostMapping
    @PreAuthorize("hasRole('CAJA')")
    public ResponseEntity<MenuDiario> crearMenu(
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        Usuario usuario = usuarioRepository
                .findByUsername(userDetails.getUsername())
                .orElseThrow(() ->
                        new BadRequestException("Usuario no encontrado")
                );

        return ResponseEntity.ok(
                menuDiarioService.crearMenuHoy(usuario)
        );
    }

    @GetMapping("/activo")
    @PreAuthorize("hasRole('CAJA')")
    public ResponseEntity<MenuDiarioResponseDTO> obtenerActivo() {
        
        MenuDiario menu = menuDiarioService.obtenerMenuActivo();
        
        return ResponseEntity.ok(
                MenuDiarioMapper.toDTO(menu)
        );

    }


}




