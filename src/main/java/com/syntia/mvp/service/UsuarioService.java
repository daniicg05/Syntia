package com.syntia.mvp.service;

import com.syntia.mvp.model.Rol;
import com.syntia.mvp.model.Usuario;
import com.syntia.mvp.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Servicio de lógica de negocio para la gestión de usuarios.
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registra un nuevo usuario con la contraseña cifrada.
     *
     * @param email    email del usuario
     * @param password contraseña en texto plano
     * @param rol      rol del usuario
     * @return usuario creado
     * @throws IllegalStateException si el email ya está registrado
     */
    public Usuario registrar(String email, String password, Rol rol) {
        if (usuarioRepository.existsByEmail(email)) {
            throw new IllegalStateException("El email ya está registrado: " + email);
        }

        Usuario usuario = Usuario.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .rol(rol)
                .build();

        return usuarioRepository.save(usuario);
    }

    /**
     * Busca un usuario por email.
     *
     * @param email email del usuario
     * @return Optional con el usuario
     */
    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    /**
     * Obtiene todos los usuarios registrados.
     *
     * @return lista de usuarios
     */
    public List<Usuario> obtenerTodos() {
        return usuarioRepository.findAll();
    }

    /**
     * Busca un usuario por ID.
     *
     * @param id ID del usuario
     * @return Optional con el usuario
     */
    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    /**
     * Elimina un usuario por ID.
     *
     * @param id ID del usuario a eliminar
     */
    public void eliminar(Long id) {
        usuarioRepository.deleteById(id);
    }
}

