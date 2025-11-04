@file:OptIn(ExperimentalForeignApi::class)

package sdl3

import cnames.structs.SDL_Window
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.internal.SDL_CreateWindow

/**
 * Represents a window in SDL.
 *
 * @param title The title of the window.
 * @param width The width of the window.
 * @param height The height of the window.
 * @param flags The window creation flags.
 */
class SDLWindow(val title: String, val width: Int, val height: Int, val flags: ULong) {

    private var ptr: CPointer<SDL_Window> =
        SDL_CreateWindow(title, width, height, flags) ?: throw RuntimeException("Failed to create SDL Window")
}
