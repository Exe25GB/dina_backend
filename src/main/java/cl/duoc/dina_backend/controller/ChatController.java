package cl.duoc.dina_backend.controller;

import cl.duoc.dina_backend.dto.Dtos.ChatDetalleDto;
import cl.duoc.dina_backend.dto.Dtos.ChatDto;
import cl.duoc.dina_backend.dto.Dtos.EnviarMensajeRequest;
import cl.duoc.dina_backend.dto.Dtos.MensajeDto;
import cl.duoc.dina_backend.model.Usuario;
import cl.duoc.dina_backend.security.UsuarioActual;
import cl.duoc.dina_backend.service.ChatService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Chat de cada solicitud aprobada ({id} = id de la solicitud).
 * Lo ven quien la pidió y los administradores.
 */
@RestController
@RequestMapping("/api/chats")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /** Abiertos y cerrados, del de actividad más reciente al más antiguo. */
    @GetMapping
    public List<ChatDto> listar(@UsuarioActual Usuario usuario) {
        return chatService.listar(usuario);
    }

    /** Ej.: /api/chats/12?despuesDe=40 → solo los mensajes nuevos (la app pregunta cada 5 s). */
    @GetMapping("/{id}")
    public ChatDetalleDto detalle(@UsuarioActual Usuario usuario, @PathVariable Long id,
                                  @RequestParam(defaultValue = "0") long despuesDe) {
        return chatService.detalle(usuario, id, despuesDe);
    }

    @PostMapping("/{id}/mensajes")
    @ResponseStatus(HttpStatus.CREATED)
    public MensajeDto enviar(@UsuarioActual Usuario usuario, @PathVariable Long id,
                             @Valid @RequestBody EnviarMensajeRequest req) {
        return chatService.enviar(usuario, id, req.texto());
    }

    /** Solo ADMIN: pasa el chat a "Cerrados". */
    @PatchMapping("/{id}/cerrar")
    public ChatDto cerrar(@UsuarioActual Usuario usuario, @PathVariable Long id) {
        return chatService.cerrar(usuario, id);
    }
}
